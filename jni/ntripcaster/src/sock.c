/* sock.c
 * - General Socket Functions
 *
 * Copyright (c) 2003
 * German Federal Agency for Cartography and Geodesy (BKG)
 *
 * Developed for Networked Transport of RTCM via Internet Protocol (NTRIP)
 * for streaming GNSS data over the Internet.
 *
 * Designed by Informatik Centrum Dortmund http://www.icd.de
 *
 * NTRIP is currently an experimental technology.
 * The BKG disclaims any liability nor responsibility to any person or entity
 * with respect to any loss or damage caused, or alleged to be caused,
 * directly or indirectly by the use and application of the NTRIP technology.
 *
 * For latest information and updates, access:
 * http://igs.ifag.de/index_ntrip.htm
 *
 * Georg Weber
 * BKG, Frankfurt, Germany, June 2003-06-13
 * E-mail: euref-ip@bkg.bund.de
 *
 * Based on the GNU General Public License published Icecast 1.3.12
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

#ifdef HAVE_CONFIG_H
#ifdef _WIN32
#include <win32config.h>
#else
#include <config.h>
#endif
#endif

#include "definitions.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

#include "definitions.h"

#include <sys/types.h>
#include <ctype.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>

#ifndef _WIN32
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/time.h>
#include <netdb.h>
#else
#include <winsock.h>
#include <io.h>
#endif

#ifdef HAVE_LIBWRAP
# include <tcpd.h>
# ifdef NEED_SYS_SYSLOG_H
#  include <sys/syslog.h>
# else
#  include <syslog.h>
# endif
int allow_severity = LOG_INFO;
int deny_severity = LOG_WARNING;
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "sock.h"
#include "connection.h"
#include "log.h"
#include "main.h"
#include "utility.h"
#include "ntrip_string.h"


#ifdef _WIN32
#define read _read
extern int running;
#else
/* extern int h_errno, errno, running;*/
extern int running;
#endif
extern server_info_t info;
extern struct in_addr localaddr;

#ifdef DEBUG_SOCKETS
	mutex_t sock_mutex;
	avl_tree *sock_sockets;
#endif

#if defined(_WIN32) || !defined(HAVE_INET_ATON)

int inet_aton(const char *s, struct in_addr *a)
{
	int lsb, b2, b3, msb;
	
	if (sscanf(s, "%d.%d.%d.%d", &lsb, &b2, &b3, &msb) < 4) {
		return 0;
	}
#if defined(HAVE_INET_ADDR)
	a->s_addr = inet_addr(s);
	
	if (a->s_addr == -1)
		return 0;
	return 1;
#else

	a->s_addr = lsb + (b2 << 8) + (b3 << 16) + (msb << 24);
	
	return 1;
#endif
}
#endif

#ifdef DEBUG_SOCKETS
ice_socket_t *
sock_find (SOCKET s) 
{
	ice_socket_t is, *out = NULL;
	
	xa_debug (4, "DEBUG: Looking for socket %d", s);

	is.sock = s;

	thread_mutex_lock (&sock_mutex);
	out = avl_find (sock_sockets, &is);
	thread_mutex_unlock (&sock_mutex);
	return out;
}
#endif

int sock_valid(const SOCKET sockfd)
{
	return ((int)sockfd >= 0);
}

/*
 * Set the socket's KEEPALIVE flag
 * this will detech idle connections from blocking
 * forever if the host crashes
 */
int sock_set_keepalive(SOCKET sockfd, const int keepalive)
{
	int optval = keepalive;
	int res;

	xa_debug (4, "DEBUG: Setting socket %d keepalive to %d", sockfd, keepalive);

	res = setsockopt(sockfd, SOL_SOCKET, SO_KEEPALIVE, (void *) &optval, sizeof (int));
	
	if (res == -1) {
		xa_debug (1, "WARNING: sock_set_keepalive() failed");
		return -1;
	}
		
#ifdef DEBUG_SOCKETS
	{
		ice_socket_t *is = sock_find (sockfd);
		if (!is) {
			android_log (ANDROID_LOG_VERBOSE, "WARNING: sock_set_keepalive() setting unknown socket");
		} else {
			is->keepalive = keepalive;
		}
	}
#endif
	
	return res;
}

#ifdef SO_LINGER
int sock_set_no_linger(SOCKET sockfd)
{
	struct linger lin = { 0, 0 };
	int res;

	xa_debug (4, "DEBUG: Setting socket %d to no linger", sockfd);

	res = setsockopt(sockfd, SOL_SOCKET, SO_LINGER, (void *) &lin, sizeof (struct linger));

	if (res == -1) {
		xa_debug (1, "WARNING: sock_set_no_linger() failed");
		return -1;
	}

# ifdef DEBUG_SOCKETS
 	{
		ice_socket_t *is = sock_find (sockfd);
		if (!is) {
			android_log (ANDROID_LOG_VERBOSE, "WARNING: sock_set_no_linger() setting unknown socket");
		} else {
			is->linger = 0;
		}
	}
# endif
	return res;
}
#endif

/* 
 * Set or the socket to blocking or nonblocking. 
 * Assert Class: 1 
 */
int sock_set_blocking(SOCKET sockfd, const int block)
{
#ifdef _WIN32
	int varblock = block;
#else
	int res;
#endif
	
	xa_debug(3, "Setting fd %d to %s", sockfd,
		 (block == SOCK_BLOCK) ? "blocking" : "nonblocking");
	
	if (!sock_valid(sockfd)) {
		xa_debug(1,
			 "ERROR: sock_set_blocking() called with invalid socket");
		return SOCKET_ERROR;
	} else if ((block < 0) || (block > 1)) {
		xa_debug(1,
			 "ERROR: sock_set_blocking() called with invalid block value");
		return SOCKET_ERROR;
	}
#ifdef _WIN32
	return ioctlsocket(sockfd, FIONBIO, &varblock);
#else
	res = fcntl(sockfd, F_SETFL, (block == SOCK_BLOCK) ? 0 : O_NONBLOCK);

	if (res == -1) {
		xa_debug (1, "WARNING: sock_set_blocking() on socket %d failed", sockfd);
		return -1;
	}

# ifdef DEBUG_SOCKETS
	{
		ice_socket_t *is = sock_find (sockfd);
		if (!is) {
			android_log (ANDROID_LOG_VERBOSE, "WARNING: sock_set_blocking(): Unknown socket %d", sockfd);
			return -1;
		} else {
			is->blocking = block;
		}
	}
# endif
	return res;
#endif
}

#ifdef DEBUG_SOCKETS

ice_socket_t *
sock_create (SOCKET s, int domain, int type, int protocol)
{
	ice_socket_t *is = (ice_socket_t *) nmalloc (sizeof (ice_socket_t));

	xa_debug (3, "DEBUG: sock_create(): Creating socket %d at %p", s, is);

	is->sock = s;
	is->domain = domain;
	is->type = type;
	is->protocol = protocol;
	is->blocking = UNKNOWN;
	is->keepalive = UNKNOWN;
	is->linger = UNKNOWN;
	is->busy = 0;

	return is;
}

int
sock_insert (ice_socket_t *is)
{
	if (!is || !sock_valid (is->sock)) {
		android_log (ANDROID_LOG_VERBOSE, "WARNING: Tried to insert NULL socket");
		return -1;
	}

	xa_debug (4, "DEBUG: inserting socket %d", is->sock);

	thread_mutex_lock (&sock_mutex);

	if (avl_insert (sock_sockets, is)) {
		android_log (ANDROID_LOG_VERBOSE, "WARNING: Tried to insert duplicate socket. Weird!");
		thread_mutex_unlock (&sock_mutex);
		return 0;
	}

	thread_mutex_unlock (&sock_mutex);
	return 1;
}

int
sock_add (SOCKET s, int domain, int type, int protocol) 
{
	ice_socket_t *is;

	xa_debug (4, "DEBUG: Adding socket %d", s);

	is = sock_create (s, domain, type, protocol);
	return sock_insert (is);
}


int
sock_del (SOCKET s)
{
	ice_socket_t *is, *ds;

	xa_debug (3, "DEBUG: sock_del(): Removing socket %d", s);

	if (!sock_valid (s)) {
		xa_debug (1, "WARNING: Tried to remove invalid socket %d", s);
		return -1;
	}



	is = sock_find (s);

	if (!is) {
		xa_debug (1, "WARNING: Tried to remove a nonlisted socket %d", s);
		return -1;
	}

	thread_mutex_lock (&sock_mutex);
	ds = avl_delete (sock_sockets, is);

	if (ds) {
		nfree (ds);
		thread_mutex_unlock (&sock_mutex);
		return 1;
	}

	thread_mutex_unlock (&sock_mutex);
	return -1;
}
#endif

SOCKET sock_socket(int domain, int type, int protocol)
{
	SOCKET s = socket(domain, type, protocol);

	xa_debug (4, "DEBUG: sock_socket() creating socket %d", s);

	if (sock_valid(s)) {
#ifdef DEBUG_SOCKETS
		sock_add (s, domain, type, protocol);
#endif
		/*
		 * Turn on KEEPALIVE to detect crashed hosts 
		 */
		sock_set_keepalive(s, 1);

#ifdef SO_LINGER
		sock_set_no_linger(s);
#endif
	}

	return s;
}

SOCKET sock_accept(SOCKET s, struct sockaddr * addr, socklen_t * addrlen)
{
	SOCKET rs = accept(s, addr, addrlen);

	xa_debug (4, "DEBUG: sock_accept() created socket %d", s);

	if (sock_valid(rs)) {
#ifdef DEBUG_SOCKETS
		sock_add (rs, AF_INET, SOCK_STREAM, 0);
#endif
		/*
		 * Turn on KEEPALIVE to detect crashed hosts 
		 */
		sock_set_keepalive(rs, 1);

#ifdef SO_LINGER
		sock_set_no_linger(rs);
#endif
	}

	return rs;
}

/* 
 * Close all sockets
 */
void
sock_close_all_sockets ()
{
	android_log (ANDROID_LOG_VERBOSE, "Closing all remaining sockets...");
	my_sleep (30000);
}

/* 
 * Close the socket 
 * Assert Class: 0
 */
int sock_close(SOCKET sockfd)
{
	xa_debug (4, "DEBUG: sock_close: Closing socket %d", sockfd);

#ifdef DEBUG_SOCKETS
	sock_del (sockfd);
#endif

#ifdef _WIN32
	if (sockfd > 1)	
		return closesocket(sockfd);
	else
		return 0;
#else
	return fd_close(sockfd);
#endif
}

/* 
 * Write len bytes from buff to the client. Kick him on network errors.
 * Return the number of bytes written and -1 on error.
 * Assert Class: 2
 * Potential problems: Any usage of errno is bad in a threaded application.
 */
int
sock_write_bytes_or_kick(SOCKET sockfd, connection_t * clicon,
			 const char *buff, const int len)
{
	int res, err;

	if (!sock_valid(sockfd)) {
		xa_debug(1,
			 "ERROR: sock_write_bytes_or_kick() called with invalid socket");
		return -1;
	} else if (!clicon) {
		xa_debug(1,
			 "ERROR: sock_write_bytes_or_kick() called with NULL client");
		return -1;
	} else if (!buff) {
		xa_debug(1,
			 "ERROR: sock_write_bytes_or_kick() called with NULL data");
		return -1;
	} else if (len <= 0) {
		xa_debug(1,
			 "ERROR: sock_write_bytes_or_kick() called with invalid length");
		return -1;
	}

	errno = 666;

	res = sock_write_bytes(sockfd, buff, len);

	err = errno;

	if (res < 0) {
		xa_debug(4, "DEBUG: sock_write_bytes_or_kick: %d err [%d]",
			 res, err);

		if (!is_recoverable(errno)) {
			kick_connection(clicon, "Client signed off");
			return -1;
		}
	}
	return res;
}

/*
 * Write len bytes from buf to the socket.
 * Returns the return value from send()
 * Assert Class: 0
 */
int sock_write_bytes(SOCKET sockfd, const char *buff, int len)
{
	int t;

	if (!buff) {
		xa_debug(1,
			 "ERROR: sock_write_bytes() called with NULL data");
		return -1;
	} else if (len <= 0) {
		xa_debug(1,
			 "ERROR: sock_write_bytes() called with zero or negative len");
		return -1;
	} else if (!sock_valid(sockfd)) {
		xa_debug(1,
			 "ERROR: sock_write_bytes() called with invalid socket");
		return -1;
	}

	for(t=0 ; len > 0 ; ) {
		int n=send(sockfd, buff+t, len, 0);
		
		if (n < 0)
		    return (t == 0) ? n : t;
		t+=n;
		len-=n;
	}

	return t;
}

/*
 * Write a string to a socket. 
 * Return 1 if all bytes where successfully written, and 0 if not.
 * Assert Class: 2
 */
int sock_write_string(SOCKET sockfd, const char *buff)
{
	int write_bytes = 0, res = 0, len = ice_strlen(buff);

	if (!sock_valid(sockfd)) {
		fprintf(stderr,
			"ERROR: sock_write_string() called with invalid socket\n");
		return -1;
	} else if (!buff) {
		fprintf(stderr,
			"ERROR: sock_write_string() called with NULL format\n");
		return -1;
	}

	/*
	 * Never use send() to sockets 0 or 1 in Win32.
	 */

	if (sockfd == 1 || sockfd == 0) {
		if (running == SERVER_RUNNING) {
			write_bytes = fprintf(stdout, "%s", buff);
			fflush(stdout);
		}
	} else {
		while (write_bytes < len) {
			res =
				send(sockfd, &buff[write_bytes],
				     len - write_bytes, 0);
			if (res < 0 && !is_recoverable(errno))
				return 0;
			if (res > 0)
				write_bytes += res;
			else
				my_sleep(30000);
		}
	}

	return (write_bytes == len ? 1 : 0);
}

/* 
 * Write a printf() style formatted message to the socket 
 * Return 1 if all bytes where successfully written, and 0 if not.
 * Assert Class: 2
 * Potential problems: Will truncate the string if longer than BUFSIZE bytes.
 */
int sock_write(SOCKET sockfd, const char *fmt, ...)
{
	char buff[BUFSIZE];
	va_list ap;

	va_start(ap, fmt);
	vsnprintf(buff, BUFSIZE, fmt, ap);
	va_end(ap);

	return sock_write_string(sockfd, buff);
}

/* 
 * Write a printf() style and newline terminated message to the socket 
 * Return 1 if all bytes where successfully written, and 0 if not.
 * Assert Class: 2
 * Potential problems: Will truncate the string if longer than BUFSIZE bytes.
 */
int sock_write_line(SOCKET sockfd, const char *fmt, ...)
{
	char buff[BUFSIZE];
	va_list ap;

	va_start(ap, fmt);
	vsnprintf(buff, BUFSIZE, fmt, ap);
	return sock_write(sockfd, "%s\r\n", buff);
}

/* 
 * Read a HTTP header. A number of lines terminated by a two newlines.
 * Reads no more than len bytes into string pointed to by buff. If the
 * return value is 1, then the string is valid and null terminated.
 * Assert Class: 3
 * Potential Problems: Usage of errno
 */
int sock_read_lines_np(SOCKET sockfd, char *buff, const int len)
{
	char c = '\0';
	int read_bytes, pos = 0;

	if (!sock_valid(sockfd)) {
		xa_debug(1,
			 "ERROR: sock_read_lines_np() called with invalid socket");
		return 0;
	} else if (!buff) {
		xa_debug(1,
			 "ERROR: sock_read_lines_np() called with NULL storage pointer");
		return 0;
	} else if (len <= 0) {
		xa_debug(1,
			 "ERROR: sock_read_lines_np() called with invalid length");
		return 0;
	}
#ifdef _WIN32
	WSASetLastError(0);
#else
	errno = 0;
#endif

	read_bytes = recv(sockfd, &c, 1, 0);

	if (read_bytes < 0) {
		xa_debug(1, "DEBUG: Socket error on socket %d %d", sockfd,
			 errno);
		return 0;
	}

	while ((read_bytes == 1) && (pos < (len - 1))) {
		if (c != '\r')
			buff[pos++] = c;
		if ((pos > 1)
		    && (buff[pos - 1] == '\n'
			&& buff[pos - 2] == '\n')) break;
#ifdef _WIN32
		WSASetLastError(0);
#else
		errno = 0;
#endif
		read_bytes = recv(sockfd, &c, 1, 0);
		if (read_bytes < 0) {
			xa_debug(1, "DEBUG: Socket error on socket %d %d",
				 sockfd, errno);
			return 0;
		}
	}

	if (read_bytes == 1) {
		buff[pos] = '\0';
		return 1;
	} else {
		return 0;
	}
}

int sock_read_lines(SOCKET sockfd, char *buff, const int len)
{
	char c[2];
	int read_bytes, pos = 0;

	if (!sock_valid(sockfd)) {
		xa_debug(1,
			 "ERROR: sock_read_lines() called with invalid socket");
		return 0;
	} else if (!buff) {
		xa_debug(1,
			 "ERROR: sock_read_lines() called with NULL storage pointer");
		return 0;
	} else if (len <= 0) {
		xa_debug(1,
			 "ERROR: sock_read_lines() called with invalid length");
		return 0;
	}

	c[0] = c[1] = '\0';

#ifdef _WIN32
	WSASetLastError(0);
#else
	errno = 0;
#endif

	read_bytes = recv(sockfd, &c[0], 1, 0);

	if (read_bytes < 0) {
		xa_debug(1, "DEBUG: Socket error on socket %d %d", sockfd,
			 errno);
		return 0;
	}

	while ((read_bytes == 1) && (pos < (len - 1))) {
		if (c[0] != '\r')
			buff[pos++] = c[0];
		if ((pos > 1)
		    && (buff[pos - 1] == '\n'
			&& buff[pos - 2] == '\n')) break;
#ifdef _WIN32
		WSASetLastError(0);
#else
		errno = 0;
#endif
		read_bytes = recv(sockfd, &c[0], 1, 0);
		if (read_bytes < 0) {
			xa_debug(1, "DEBUG: Socket error on socket %d %d",
				 sockfd, errno);
			return 0;
		}
	}

	if (read_bytes == 1) {
		buff[pos] = '\0';
		return 1;
	} else {
		return 0;
	}
}

/*
 * Create a socket for all incoming requests on specified port.
 * If info.myhostname is NULL, bind it to INADDR_ANY (all available interfaces).
 * If info.myhostname is not NULL, resolv it (if needed), and bind to that.
 * Return the socket for bound socket, or INVALID_SOCKET if failed.
 * Assert Class: 3
 */
SOCKET sock_get_server_socket(const int port)
{
	struct sockaddr_in sin;
	int sin_len, error;
	SOCKET sockfd;

	if (port < 0) {
		android_log(ANDROID_LOG_VERBOSE,
			  "ERROR: Invalid port number %d. Cannot listen for requests, this is bad!",
			  port);
		return INVALID_SOCKET;
	}

	xa_debug (2, "DEBUG: Getting socket for port %d", port);

	/*
	 * get socket descriptor 
	 */
	sockfd = sock_socket (AF_INET, SOCK_STREAM, 0);
	if (sockfd == INVALID_SOCKET)
		return INVALID_SOCKET;

	/*
	 * Setup socket 
	 */
#ifdef HAVE_SETSOCKOPT
	{
		int tmp = 1;

		if (setsockopt
		    (sockfd, SOL_SOCKET, SO_REUSEADDR, (const void *) &tmp,
		     sizeof (tmp)) != 0)
			android_log(ANDROID_LOG_VERBOSE,
				  "ERROR: setsockopt() failed to set SO_REUSEADDR flag. (mostly harmless)");
	}
#endif

	/*
	 * setup sockaddr structure 
	 */
	sin_len = sizeof (sin);
	memset(&sin, 0, sin_len);
	sin.sin_family = AF_INET;

	if (info.myhostname != NULL && info.myhostname[0]) {
		if (isdigit((int) info.myhostname[0])
		    && isdigit((int) info.
			       myhostname[ice_strlen(info.myhostname) - 1])) {
			if (inet_aton
			    (info.myhostname,
			     (struct in_addr *) &localaddr) == 0) {
				android_log(ANDROID_LOG_VERBOSE,
					  "ERROR: Invalid ip number %s, will die now",
					  info.myhostname);
				clean_shutdown(&info);
			}
			sin.sin_addr.s_addr = localaddr.s_addr;
		} else {
			struct hostent *hostinfoptr, hostinfo;
			char buf[BUFSIZE];
			int error;

			hostinfoptr =
				ice_gethostbyname(info.myhostname, &hostinfo,
						  buf, BUFSIZE, &error);
			if (hostinfoptr == NULL) {
				android_log(ANDROID_LOG_VERBOSE,
					  "Unknown host %s, that's it for me!",
					  info.myhostname);
				ice_clean_hostent();
				clean_shutdown(&info);
			}
			sin.sin_addr.s_addr = localaddr.s_addr;
			memcpy((void *) &localaddr, hostinfoptr->h_addr,
			       sizeof (localaddr));
			ice_clean_hostent();
		}
	} else {
		sin.sin_addr.s_addr = htonl(INADDR_ANY);
	}

	sin.sin_port = htons(port);

	/*
	 * bind socket to port 
	 */
	error = bind(sockfd, (struct sockaddr *) &sin, sin_len);
	if (error == SOCKET_ERROR) {
		android_log(ANDROID_LOG_VERBOSE,
			  "Bind to socket on port %d failed. Shutting down now.",
			  port);
		clean_shutdown(&info);
	}

	return sockfd;
}


/*
 * Connect to hostname on specified port and return the created socket.
 * Assert Class: 3
 */
SOCKET sock_connect_wto(const char *hostname, const int port,
			const int timeout)
{
	SOCKET sockfd;
	struct sockaddr_in sin, server;
	struct hostent *host;
	struct hostent hostinfo;
	char buf[BUFSIZE];
	int error;

	if (!hostname || !hostname[0]) {
		android_log(ANDROID_LOG_VERBOSE,
			  "ERROR: sock_connect() called with NULL or empty hostname");
		return INVALID_SOCKET;
	} else if (port <= 0) {
		android_log(ANDROID_LOG_VERBOSE,
			  "ERROR: sock_connect() called with invalid port number");
		return INVALID_SOCKET;
	}

	sockfd = sock_socket(AF_INET, SOCK_STREAM, 0);
	if (sockfd == INVALID_SOCKET) {
		sock_close(sockfd);
		return INVALID_SOCKET;
	}

	if (info.myhostname != NULL) {
		struct sockaddr_in localsin;
		memset(&localsin, 0, sizeof (struct sockaddr_in));

		xa_debug(2, "DEBUG: Trying to bind to %s", info.myhostname);

		localsin.sin_addr = localaddr;
		localsin.sin_family = AF_INET;
		localsin.sin_port = 0;

		if (bind
		    (sockfd, (struct sockaddr *) &localsin,
		     sizeof (localsin)) == SOCKET_ERROR) {
			xa_debug(2, "DEBUG: Unable to bind", info.myhostname);
			android_log(ANDROID_LOG_VERBOSE,
				  "ERROR: Bind to local address %s failed",
				  info.myhostname);
			sock_close(sockfd);
			return INVALID_SOCKET;
		}
	}

	memset(&sin, 0, sizeof (sin));
	memset(&server, 0, sizeof (struct sockaddr_in));

	if (isdigit((int) hostname[0])
	    && isdigit((int) hostname[ice_strlen(hostname) - 1])) {
		if (inet_aton(hostname, (struct in_addr *) &sin.sin_addr) ==
		    0) {
			android_log(ANDROID_LOG_VERBOSE, "ERROR: Invalid ip number %s",
				  hostname);
			sock_close(sockfd);
			return INVALID_SOCKET;
		}
		memcpy(&server.sin_addr, &sin.sin_addr, sizeof (sin));
	} else {
		host =
			ice_gethostbyname(hostname, &hostinfo, buf, BUFSIZE,
					  &error);
		if (host == NULL) {
			xa_debug(1, "DEBUG: gethostbyname %s failed",
				 hostname);
			sock_close(sockfd);
			ice_clean_hostent();
			return INVALID_SOCKET;
		}
		memcpy(&server.sin_addr, host->h_addr, host->h_length);
		ice_clean_hostent();
	}

	server.sin_family = AF_INET;
	server.sin_port = htons(port);

	{
		char buf[50];

		makeasciihost(&server.sin_addr, buf);
		xa_debug(1, "Trying to connect to %s:%d", buf, port);
	}

	if (timeout > 0) {
		fd_set wfds;
		struct timeval tv;
		int retval;
		int val;
		socklen_t valsize = sizeof (int);

		xa_debug(3,
			 "DEBUG: sock_connect(): doing a connection w/ timeout");

		FD_ZERO(&wfds);
		FD_SET(sockfd, &wfds);
		tv.tv_sec = timeout;
		tv.tv_usec = 0;

		sock_set_blocking(sockfd, SOCK_NONBLOCK);
		retval = connect(sockfd, (struct sockaddr *) &server, sizeof (server));
		if (retval == 0) {
			xa_debug(3, "DEBUG: sock_connect(): non blocking connect returned 0!");
			sock_set_blocking(sockfd, SOCK_BLOCK);
			return sockfd;
		} else {
#ifdef _WIN32
			if (WSAGetLastError() == WSAEINPROGRESS) {
#else
			if (!is_recoverable(errno)) {
#endif
				xa_debug(3, "DEBUG: sock_connect(): connect didn't return EINPROGRESS!, was: %d", errno);
				sock_close(sockfd);
				return SOCKET_ERROR;
			}
		}

		if (select(sockfd + 1, NULL, &wfds, NULL, &tv)) {
			retval = getsockopt(sockfd, SOL_SOCKET, SO_ERROR,
					    (void *) &val,
					    (socklen_t *) & valsize);
			if ((retval == 0) && (val == 0)) {
				sock_set_blocking(sockfd, SOCK_BLOCK);
				return sockfd;
			} else {
				xa_debug(3,
					 "DEBUG: sock_connect(): getsockopt returned %i, val = %i, valsize = %i, errno = %i!",
					 retval, val, valsize, errno);
				sock_close(sockfd);
				return SOCKET_ERROR;
			}
		} else {
			xa_debug(3,
				 "DEBUG: sock_connect(): select returned 0");
			sock_close(sockfd);
			return SOCKET_ERROR;
		}
	} else {
		if (connect
		    (sockfd, (struct sockaddr *) &server,
		     sizeof (server)) == 0) {
			return sockfd;
		} else {
			sock_close(sockfd);
			return SOCKET_ERROR;
		}
	}
}

#ifdef HAVE_LIBWRAP
static const char libwraptype[4][20] =
	{ "NtripCaster_client", "NtripCaster_source", "NtripCaster_admin", "NtripCaster" };

/*
 * Return the corresponding string for libwrap for the specified connection type.
 * Assert Class: 1
 */
const char *sock_get_libwrap_type(const contype_t contype)
{
	switch (contype) {
		case client_e:
			return libwraptype[0];
			break;
		case source_e:
			return libwraptype[1];
			break;
		case admin_e:
			return libwraptype[2];
			break;
		default:
			return libwraptype[3];
			break;
	}
}

/* 
 * Checks whether the host connected to the sock socket is allowed in or not.
 * Assert Class: 1
 */
int sock_check_libwrap(const int sock, const contype_t contype)
{
	struct request_info req;

	if (!sock_valid(sock)) {
		xa_debug(1,
			 "ERROR: sock_check_libwrap() called with invalid socket");
		return 0;
	}

	xa_debug(2, "Checking socket %d for validation", sock);

	request_init(&req, RQ_DAEMON, sock_get_libwrap_type(contype), RQ_FILE,
		     sock, NULL);
	fromhost(&req);
	if (!hosts_access(&req))
		return 0;
	return 1;
}
#endif

char *sock_get_local_ipaddress()
{
#ifndef _WIN32
	SOCKET sockfd;
	socklen_t sinlen = sizeof (struct sockaddr_in);
	struct sockaddr_in sin, cliaddr;

	sockfd = sock_socket(AF_INET, SOCK_DGRAM, 0);

	memset(&sin, 0, sizeof (sin));
	sin.sin_family = AF_INET;
	sin.sin_port = htons(info.port[0]);

	if (!inet_aton("130.240.1.1", (struct in_addr *) &sin.sin_addr)) {
		xa_debug(1, "DEBUG: inet_aton() failed with [%d]", errno);
		return nstrdup("dynamic");
	}

	if (connect(sockfd, (struct sockaddr *) &sin, sizeof (sin)) == -1) {
		xa_debug(1, "DEBUG: connect() failed with [%s]", errno);
		return nstrdup("dynamic");
	}


	if (getsockname(sockfd, (struct sockaddr *) &cliaddr, &sinlen) == 0) {
		close(sockfd);
		if (inet_ntoa(cliaddr.sin_addr))
			return nstrdup(inet_ntoa(cliaddr.sin_addr));
		else
			return nstrdup("dynamic");
	} else {
		xa_debug(1, "DEBUG: getsockname() failed with [%d]", errno);
		close(sockfd);
		return nstrdup("dynamic");
	}
#else
	return nstrdup("dynamic");
#endif
}
