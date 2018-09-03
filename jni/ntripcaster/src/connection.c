/* connection.c
 * - Connection functions
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

#ifdef HAVE_SIGNAL_H
#include <signal.h>
#endif

#include <stdlib.h>
#include <stdarg.h>
#include <string.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#include <sys/types.h>
#include <ctype.h>
#include <errno.h>
#include <time.h>
#include <fcntl.h>

#ifndef _WIN32
#include <sys/socket.h>
#include <sys/wait.h>
#include <netinet/in.h>
#include <sys/time.h>
#else
#include <winsock.h>
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "utility.h"
#include "ntrip_string.h"
#include "connection.h"
#include "log.h"
#include "sock.h"
#include "client.h"
#include "source.h"


/* pool.c. ajd *************************************/
# ifndef __USE_BSD
#  define __USE_BSD
# endif
#ifndef __EXTENSIONS__
#define __EXTENSIONS__
#endif
#ifdef HAVE_ASSERT_H
#include <assert.h>
#endif

/* ice_resolv.c. ajd *********************************/

#ifndef _WIN32
#include <arpa/inet.h>
#include <netdb.h>
#endif
#ifdef HAVE_LIBWRAP
# include <tcpd.h>
# ifdef NEED_SYS_SYSLOG_H
#  include <sys/syslog.h>
# else
#  include <syslog.h>
# endif
#endif
#include "main.h"


extern server_info_t info;
const char cnull[] = "(null)";

/* pool.c. ajd *************************************/
static mutex_t pool_mutex = {MUTEX_STATE_UNINIT};
static avl_tree *pool = NULL;

/* ice_resolv.c. ajd *******************************/
#ifdef _WIN32
extern int running;
#else
/* extern int volatile h_errno,errno, running; */
#endif
extern struct in_addr localaddr;

/*
 * This is called to handle a brand new connection, in it's own thread.
 * Nothing is know about the type of the connection.
 * Assert Class: 3
 */
void *handle_connection(void *arg)
{
	connection_t *con = (connection_t *)arg;
	char line[BUFSIZE] = "";
	int res;

	thread_init();

	if (!con) {
		android_log(ANDROID_LOG_VERBOSE, "handle_connection: got NULL connection");
		thread_exit(0);
	}

	if (info.reverse_lookups)
		con->hostname = reverse(con->host);

	sock_set_blocking(con->sock, SOCK_BLOCK);

	/* Fill line[] with the user header, ends with \n\n */
	if ((res = sock_read_lines(con->sock, line, BUFSIZE)) <= 0) {
		android_log(ANDROID_LOG_VERBOSE, "Socket error on connection %lu", con->id);
		kick_not_connected(con, "Socket error");
		thread_exit(0);
	}

	if (ice_strncmp(line, "GET", 3) == 0) {
		client_login(con, line);
	} else if (ice_strncmp(line, "SOURCE", 6) == 0 || ice_strncmp(line, "POST", 4) == 0) {
		source_login (con, line);
	} else {
		write_400 (con);
		kick_not_connected(con, "Invalid header");
	}

	thread_exit(0);
	return NULL;
}

connection_t *
create_connection()
{
	connection_t *con = (connection_t *) nmalloc (sizeof (connection_t));
	con->type = unknown_connection_e;
	con->sin = NULL;
	con->hostname = NULL;
	con->headervars = NULL;
	con->food.source = NULL;
	con->user = NULL;
	return con;
}

connection_t *
get_connection (sock_t *sock)
{
	int sockfd;
	socklen_t sin_len;
	connection_t *con;
	fd_set rfds;
	struct timeval tv;
	int i, maxport = 0;
	struct sockaddr_in *sin = (struct sockaddr_in *)nmalloc(sizeof(struct sockaddr_in));

	if (!sin)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: Weird stuff in get_connection. nmalloc returned NULL sin");
		return NULL;
	}

	/* setup sockaddr structure */
	sin_len = sizeof(struct sockaddr_in);
	memset(sin, 0, sin_len);

	/* try to accept a connection */
	FD_ZERO(&rfds);

	for (i = 0; i < MAXLISTEN; i++) {
		if (sock_valid (sock[i])) {
			FD_SET(sock[i], &rfds);
			if (sock[i] > maxport)
				maxport = sock[i];
		}
	}
	maxport += 1;

	tv.tv_sec = 0;
	tv.tv_usec = 30000;

	if (select(maxport, &rfds, NULL, NULL, &tv) > 0) {
		for (i = 0; i < MAXLISTEN; i++) {
			if (sock_valid (sock[i]) && FD_ISSET(sock[i], &rfds))
				break;
		}
	} else {
		nfree(sin);
		return NULL;
	}

	sockfd = sock_accept(sock[i], (struct sockaddr *)sin, &sin_len);

	if (sockfd >= 0) {
		con = create_connection();
		if (!sin)
		{
			xa_debug (1, "ERROR: NULL sockaddr struct, wft???");
			return NULL;
		}

		con->host = create_malloced_ascii_host(&(sin->sin_addr));
		con->sock = sockfd;
		con->sin = sin;
		con->sinlen = sin_len;
		xa_debug (2, "DEBUG: Getting new connection on socket %d from host %s", sockfd, con->host ? con->host : "(null)");
		con->hostname = NULL;
		con->headervars = NULL;
		con->id = new_id ();
		con->connect_time = get_time ();
#ifdef HAVE_LIBWRAP
		if (!sock_check_libwrap(sockfd, unknown_connection_e))
		{
			kick_not_connected (con, "Access Denied (tcp wrappers) [generic connection]");
			return NULL;
		}
#endif

		return con;
	}

	if (!is_recoverable (errno))
		xa_debug (1, "WARNING: accept() failed with on socket %d, max: %d, [%d:%s]", sock[i], maxport,
			  errno, strerror(errno));
	nfree (sin);
	return NULL;
}

const char *
get_user_agent (connection_t *con)
{
	const char *res;

	if (!con)
		return cnull;

	res = get_con_variable (con, "User-Agent");

	if (!res)
		res = get_con_variable (con, "User-agent");

	if (!res)
		res = get_con_variable (con, "user-agent");

        if (!res)
		return cnull;
	else
		return res;
}

/* pool.c. ajd *********************************************************************/

/* Initialize the connection pool.
 * No possible errors.
 * Assert Class: 1
 */
void
pool_init ()
{
	xa_debug (1, "DEBUG: Initializing Connection Pool.");
	thread_create_mutex (&pool_mutex);
	pool = avl_create (compare_connection, &info);
}

/* Shutdown the connection pool.
 * No possible errors.
 * Assert Class: 1
 */
void
pool_shutdown ()
{
	xa_debug (1, "DEBUG: Closing the pool.");
	pool_cleaner ();
	avl_destroy (pool, NULL);
	xa_debug (1, "DEBUG: Pool closed.");
}

/*
 * Add a connection to the connection pool.
 * Possible error codes:
 * ICE_ERROR_NOT_INITIALIZED
 * ICE_ERROR_NULL - Argument was NULL
 * Assert Class: 3
 */
int
pool_add (connection_t *con)
{
	if (!con)
		return ICE_ERROR_NULL;

	if ((pool_mutex.thread_id == MUTEX_STATE_UNINIT) || pool == NULL) {
		xa_debug (1, "WARNING: Tried to use an unitialized pool");
		return ICE_ERROR_NOT_INITIALIZED;
	}

	/* Acquire mutex lock */
	pool_lock_write ();

	/* Throw connection into the pool */
	if (avl_replace (pool, con) != NULL)
		xa_debug (1, "WARNING: Duplicate connections in the pool (id = %d)", con->id);

	/* Release mutex lock */
	pool_unlock_write ();

	return OK;
}

/*
 * Called from a source, who wants to see if the pool has any connections
 * in stock for it.
 * Returns NULL on errors.
 * Assert Class: 3
 */
connection_t *
pool_get_my_clients (const source_t *source)
{
	avl_traverser trav = {0};
	connection_t *clicon = NULL;

	if (!source) {
		xa_debug (1, "WARNING: pool_get_my_clients() called with NULL source!");
		return NULL;
	}

	/* Acquire mutex lock */
	pool_lock_write ();

	/* Search for clients for this source */
	while ((clicon = avl_traverse (pool, &trav)))
		if (clicon->food.client->source == source)
			break;

	/* If found, remove it from the pool */
	if (clicon)
		if (avl_delete (pool, clicon) == NULL)
			xa_debug (1, "WARNING: pool_get_my_clients(): Connection Pool Security Comprimised!");

	/* Release mutex lock */
	pool_unlock_write ();

	return clicon;
}

void
pool_lock_write ()
{
	assert (pool_mutex.thread_id != MUTEX_STATE_UNINIT);
	internal_lock_mutex (&pool_mutex);
}

void
pool_unlock_write ()
{
	assert (pool_mutex.thread_id != MUTEX_STATE_UNINIT);
	internal_unlock_mutex (&pool_mutex);
}

void
pool_cleaner ()
{

}


/* ice_resolv.c. ajd *********************************************************************/

struct hostent *
ice_gethostbyname (const char *hostname, struct hostent *res, char *buffer, int buflen, int *error)
{
	switch (info.resolv_type)
	{
#ifdef SOLARIS_RESOLV_OK
		case solaris_gethostbyname_r_e:
			xa_debug (2, "Resolving %s using solaris reentrant type function", hostname);
			return solaris_gethostbyname_r (hostname, res, buffer, buflen, error);
			break;
#endif
#ifdef LINUX_RESOLV_OK
		case linux_gethostbyname_r_e:
			xa_debug (2, "Resolving %s using linux reentrant type function", hostname);
			return linux_gethostbyname_r (hostname, res, buffer, buflen, error);
			break;
#endif
		case standard_gethostbyname_e:
			xa_debug (2, "Resolving %s using standard nonreentrant type function", hostname);
			return standard_gethostbyname (hostname, res, buffer, buflen, error);
			break;
		default:
			xa_debug (1, "DEBUG: gethostbyname (%s) failed cause no resolv function was defined (%d)", hostname,
				  info.resolv_type);
			return NULL;
			break;
	}
}

struct hostent *
ice_gethostbyaddr (const char *host, int hostlen, struct hostent *he, char *buffer, int buflen, int *error)
{
	char outhost[20];
	makeasciihost ((struct in_addr *)host, outhost);

	switch (info.resolv_type)
	{
#ifdef SOLARIS_RESOLV_OK
		case solaris_gethostbyname_r_e:
			xa_debug (2, "Resolving %s using solaris reentrant type function", outhost);
			return solaris_gethostbyaddr_r (host, hostlen, he, buffer, buflen, error);
			break;
#endif
#ifdef LINUX_RESOLV_OK
		case linux_gethostbyname_r_e:
			xa_debug (2, "Resolving %s using linux reentrant type function", outhost);
			return linux_gethostbyaddr_r (host, hostlen, he, buffer, buflen, error);
			break;
#endif
		case standard_gethostbyname_e:
			xa_debug (2, "Resolving %s using standard nonreentrant type function", outhost);
			return standard_gethostbyaddr (host, hostlen, he, buffer, buflen, error);
			break;
		default:
			xa_debug (1, "DEBUG: gethostbyaddr (%s) failed cause no resolv function was defined", outhost);
			return NULL;
			break;
	}
}

#ifdef SOLARIS_RESOLV_OK
struct hostent *
solaris_gethostbyname_r (const char *hostname, struct hostent *res, char *buffer, int buflen, int *error)
{
	*error = 0;

	return gethostbyname_r (hostname, res, buffer, buflen, error);
}
struct hostent *
solaris_gethostbyaddr_r (const char *host, int hostlen, struct hostent *he, char *buffer, int buflen, int *error)
{
	*error = 0;
	return gethostbyaddr_r (host, hostlen, AF_INET, he, buffer, buflen, error);
}
#endif

#ifdef LINUX_RESOLV_OK
struct hostent *
linux_gethostbyname_r (const char *hostname, struct hostent *res, char *buffer, int buflen, int *error)
{
	*error = 0;

	if (gethostbyname_r (hostname, res, buffer, buflen, &res, error) >= 0)
		return res;
	else
		return NULL;
}

struct hostent *
linux_gethostbyaddr_r (const char *host, int hostlen, struct hostent *he, char *buffer, int buflen, int *error)
{
	int out;
	*error = 0;
	if ((out = gethostbyaddr_r (host, hostlen, AF_INET, he, buffer, buflen, &he, error) >= 0))
	{
		return he;
	}
	xa_debug (2, "gethostbyaddr_r() returned %d, error is %d", out, *error);
	return NULL;
}
#endif

struct hostent *
standard_gethostbyname(const char *hostname, struct hostent *res, char *buffer, int buflen, int *error)
{
	thread_mutex_lock(&info.resolvmutex);
	*error = 0;

	res = gethostbyname(hostname);
	if (!res) {
		xa_debug(1, "DEBUG: gethostbyname (%s) failed", hostname);
		*error = errno;
	}
	return res;
}

struct hostent *
standard_gethostbyaddr(const char *host, int hostlen, struct hostent *he, char *buffer, int buflen, int *error)
{
	*error = 0;
	thread_mutex_lock(&info.resolvmutex);
	he = gethostbyaddr(host, hostlen, AF_INET);
	*error = errno;
	return he;
}

void
ice_clean_hostent()
{
	/* When not using reentrant versions of gethostbyname, this
	   mutex is locked before calling gethostbyname() and therefore, unlock it here. */
	if (info.resolv_type == standard_gethostbyname_e)
		thread_mutex_unlock (&info.resolvmutex);
}

char *
reverse (const char *host)
{
  struct hostent hostinfo, *hostinfoptr;
  struct in_addr addr;
  int error;
  char *outhost;
  char buffer[BUFSIZE];

  if (!host)
  {
	  android_log (ANDROID_LOG_VERBOSE, "ERROR: reverse() called with NULL host");
	  return NULL;
  }

  xa_debug (1, "reverse() reverse resolving %s", host);

  if (inet_aton (host, &addr))
  {
	  hostinfoptr = ice_gethostbyaddr((char *) &addr, sizeof (struct in_addr), &hostinfo, buffer, BUFSIZE, &error);

	  if (hostinfoptr && hostinfoptr->h_name)
		  outhost = nstrdup (hostinfoptr->h_name);
	  else
		  outhost = NULL;

	  ice_clean_hostent ();
	  return outhost;
  }
  else
	  return NULL;
}

char *
forward (const char *name, char *target)
{
	struct hostent hostinfo, *hostinfoptr;
	struct sockaddr_in sin;
	char buf[BUFSIZE];
	int error;

	xa_debug (1, "forward() resolving %s", name);

	if (isdigit ((int)name[0]) && isdigit ((int)name[strlen(name) - 1]))
		return NULL; /* No point in resolving ip's */

	hostinfoptr = ice_gethostbyname (name, &hostinfo, buf, BUFSIZE, &error);

	if (!hostinfoptr)
	{
		ice_clean_hostent();
		return NULL;
	}

	memset (&sin, 0, sizeof (sin));

	sin.sin_addr.s_addr = *(unsigned long *)hostinfoptr->h_addr_list[0];

	makeasciihost(&sin.sin_addr, target);

	ice_clean_hostent();

	return target;
}


