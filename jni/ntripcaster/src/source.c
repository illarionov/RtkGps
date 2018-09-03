/* source.c
 * - Source functions
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
#include "definitions.h"

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
#include <fcntl.h>

#ifndef _WIN32
#include <sys/socket.h>
#include <sys/wait.h>
#include <netinet/in.h>
#else
#include <io.h>
#include <winsock.h>
#define write _write
#define read _read
#define close _close
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "utility.h"
#include "ntrip_string.h"
#include "source.h"
#include "sock.h"
#include "log.h"
#include "connection.h"
#include "main.h"
#include "timer.h"
#include "client.h"

/* in microseconds */
#define READ_RETRY_DELAY 400
#define READ_TIMEOUT 16000

extern int running;
extern server_info_t info;

void source_login(connection_t *con, char *expr)
{
	char line[BUFSIZE], command[BUFSIZE], arg[BUFSIZE];
	char pass[BUFSIZE] ="";
	int go_on = 2;
	int connected = 1;
	int password_accepted = 0;
	source_t *source;
	char *res;

	if (con->type == unknown_connection_e)
	{
		put_source(con);
		con->food.source->type = encoder_e;
		xa_debug (2, "DEBUG: Encoder logging in with [%s]", expr);
	} else {
		xa_debug (2,"DEBUG: Icy encoder logging in with [%s]", expr);
	}
	source = con->food.source;
	source->source_agent = NULL;

	do {
		command[0] = '\0';
		arg[0] = '\0';

		if (splitc(line, expr, '\n') == NULL) {
			strncpy (line, expr, BUFSIZE);
			go_on = go_on - 1;

			if ((go_on > 0) && (!source->protocol == icy_e && source->type == encoder_e))
			{
				sock_read_lines_np (con->sock, expr, BUFSIZE);
			}
		}

		/* The delimiter on the first line is ' ', on the following lines ':' */
		if (go_on == 2)
			res = splitc (command, line, ' ');
		else
			res = splitc (command, line, ':');

		if (!res)
		{
			strncpy (command, line, BUFSIZE);
			arg[0] = '\0';
		} else {
			strncpy (arg, line, BUFSIZE);
		}

		if (line[0])
			xa_debug (2, "DEBUG: Source line: [%s] [%s]", command, arg);

		if (ice_strncmp(command, "SOURCE", 6) == 0)
		{
			if (splitc(pass, arg, ' ') == NULL) {
				sock_write_line (con->sock, "ERROR - Missing Mountpoint\r\n");
				kick_connection (con, "No Mountpoint supplied");
				connected = 0;
				return;
			}

			if (!password_match(info.encoder_pass, pass)) {
				sock_write_line (con->sock, "ERROR - Bad Password\r\n");
				kick_connection (con, "Bad Password");
				return;
			} else
				password_accepted = 1;


			if (!source->audiocast.mount)
				source->audiocast.mount = my_strdup(arg);

			{
				char slash[BUFSIZE];
				if (source->audiocast.mount[0] != '/')
				{
					snprintf(slash, BUFSIZE, "/%s", source->audiocast.mount);
					nfree (source->audiocast.mount);
					source->audiocast.mount = my_strdup (slash);
				}
			}

			if (mount_exists (source->audiocast.mount) || (source->audiocast.mount[0] == '\0')) {
				sock_write_line (con->sock, "ERROR - Mount Point Taken or Invalid\r\n");
				kick_connection (con, "Invalid Mount Point");
				return;
			}
			if (source->type == encoder_e) go_on = 1;
		}

		else if (ice_strncmp(command, "POST", 4) == 0)
		{
			if (splitc(pass, arg, ' ') == NULL) {
				sock_write_line (con->sock, "ERROR - Missing Mountpoint\r\n");
				kick_connection (con, "No Mountpoint supplied");
				connected = 0;
				return;
			}
			source->audiocast.mount = my_strdup (pass);
			if (mount_exists (source->audiocast.mount) || (source->audiocast.mount[0] == '\0')) {
				sock_write_line (con->sock, "ERROR - Mount Point Taken or Invalid\r\n");
				kick_connection (con, "Invalid Mount Point");
				return;
			}
			if (source->type == encoder_e) go_on = 1;
		}

		else if (ice_strncmp(command, "Authorization", 13) == 0)
		{
			if (splitc(pass, arg, ' ') == NULL) {
				sock_write_line (con->sock, "ERROR - Missing Mountpoint\r\n");
				kick_connection (con, "No Mountpoint supplied");
				connected = 0;
				return;
			}
			if (!password_match(info.encoder_pass, arg)) {
				sock_write_line (con->sock, "ERROR - Bad Password\r\n");
				kick_connection (con, "Bad Password");
				return;
			} else {
				password_accepted = 1;
			}
		}

		else if (strncasecmp(command, "Source-Agent", 12) == 0 || strncasecmp(command, "User-Agent", 10) == 0)
		{
			source->source_agent = my_strdup(arg);
		}
	} while ((go_on > 0) && connected);

	if (!source->source_agent || strncasecmp(source->source_agent, "ntrip", 5) != 0) {
		sock_write_line (con->sock, "Not authorized (no NTRIP source)\r\n");
		kick_connection (con, "No NTRIP source");
		return;
	}

	if (connected) {

		if ((info.num_sources + 1) > info.max_sources)
		{
			sock_write_line (con->sock, "ERROR - Too many sources\r\n");
			kick_connection (con, "Server Full (too many streams)");
			return;
		}

		add_source ();
		sock_write_line (con->sock, "OK");
		source->connected = SOURCE_CONNECTED;

		android_log (ANDROID_LOG_VERBOSE, "Accepted encoder on mountpoint %s from %s. %d sources connected",
			   source->audiocast.mount, con_host (con), info.num_sources);

		thread_mutex_lock(&info.source_mutex);
		avl_insert(info.sources, con);
		thread_mutex_unlock(&info.source_mutex);

			/* change thread name */
		thread_rename("Source Thread");
		source_func(con);
	}

	android_log (ANDROID_LOG_VERBOSE, "WARNING: Thread exiting in source_login(), this should not happen");
	thread_exit(0);
}

/* This function is started as a new thread for every connected and accepted source.
   Either it gets killed by another thread, using the kick_connection (thiscon,..),
   which should set the connected value to SOURCE_KILLED, and let this thread
   exit on it's own with the close_connection at the end.
   Or it kills itself, when the source dies, and then it should call kick_connection (thiscon,..)
   on itself, setting the value of connected to SOURCE_KILLED, and exit through close_connection () */
void *
source_func(void *conarg)
{
	source_t *source;
	avl_traverser trav = {0};
	connection_t *clicon, *con = (connection_t *)conarg;
	mythread_t *mt;
	int i;

	source = con->food.source;
	con->food.source->thread = thread_self();
	thread_init();

	mt = thread_get_mythread ();

	sock_set_blocking(con->sock, SOCK_NONBLOCK);

	while (thread_alive (mt) && ((source->connected == SOURCE_CONNECTED) || (source->connected == SOURCE_PAUSED)))
	{
		source_get_new_clients (source);

		add_chunk(con);

		for (i = 0; i < 10; i++) {

			if (source->connected != SOURCE_CONNECTED)
				break;

			thread_mutex_lock(&source->mutex);

			zero_trav (&trav);

			while ((clicon = avl_traverse(source->clients, &trav)) != NULL) {

				if (source->connected == SOURCE_KILLED || source->connected == SOURCE_PAUSED)
					break;

				source_write_to_client (source, clicon);

			}

			thread_mutex_unlock(&source->mutex);

			if (mt->ping == 1)
				mt->ping = 0;
		}

		thread_mutex_lock (&info.double_mutex);

		thread_mutex_lock (&source->mutex);
		kick_dead_clients (source);
		thread_mutex_unlock (&source->mutex);

		thread_mutex_unlock(&info.double_mutex);
	}

	thread_mutex_lock (&info.double_mutex);
	thread_mutex_lock (&info.source_mutex);
	thread_mutex_lock (&source->mutex);

	source_get_new_clients (source);

	close_connection (con, &info);

	thread_mutex_unlock (&info.source_mutex);
	thread_mutex_unlock (&info.double_mutex);

	thread_exit (0);
	return NULL;
}

source_t *
create_source()
{
	source_t *source = (source_t *)nmalloc(sizeof(source_t));
	memset(source,0,sizeof(source_t));
	source->type = unknown_source_e;
	return source;
}

void
put_source(connection_t *con)
{
	register int i;
	socklen_t sin_len;
	source_t *source = create_source();

	con->food.source = source;
	zero_stats (&source->stats);
	source->connected = SOURCE_UNUSED;
	source->type = unknown_source_e;
	thread_create_mutex(&source->mutex);
	source->audiocast.mount = NULL;
	source->cid = 0;
	source->clients = avl_create (compare_connection, &info);
	source->num_clients = 0;
	source->priority = 0;
	source->source_agent = NULL;

	for (i = 0; i < CHUNKLEN; i++)
	{
		source->chunk[i].clients_left = 0;
		source->chunk[i].len = 0;
		source->chunk[i].metalen = 0;
	}
	sin_len = 1;

	con->type = source_e;
}

void
add_source ()
{
	info.num_sources++;
	info.hourly_stats.source_connections++;
}

void
del_source ()
{
	info.num_sources--;
}

/* Must have mount, source and double mutex to call this */
connection_t *
find_mount_with_req (request_t *req)
{
	char tempbuf[256] = "";

	connection_t *con;
//	request_t search;

	int true = 0;

	avl_traverser trav = {0};

	if (!req || !req->path || !req->host)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: find_mount_with_req called with NULL request!");
		return NULL;
	}

	xa_debug (1, "DEBUG: Looking for [%s] on host [%s] on port %d", req->path, req->host, req->port);


	while ((con = avl_traverse(info.sources, &trav)) != NULL)
	{
		true = 0;

		xa_debug(2, "DEBUG: Looking on mount [%s]", con->food.source->audiocast.mount);

/* old mount search. ajd
		zero_request(&search);
		generate_http_request(con->food.source->audiocast.mount, &search);
		strncpy(tempbuf, con->food.source->audiocast.mount, 252);
		if 	(search.path[0] &&
			(ice_strcasecmp(search.host, req->host) == 0) &&
			(search.port == req->port) &&
			(	(ice_strcmp(search.path, req->path) == 0) ||
				(ice_strcmp(tempbuf, req->path) == 0))
			) {
				true = 1;
		} else if (con->food.source->audiocast.mount[0] == '/') {
			if 	((hostname_local(req->host) &&
				((ice_strcmp(con->food.source->audiocast.mount, req->path) == 0) ||
					(ice_strcmp(tempbuf, req->path) == 0)))
				)
					true = 1;
		} else {
			if (	(hostname_local(req->host) &&
				((ice_strcmp(con->food.source->audiocast.mount, req->path + 1) == 0) ||
				(ice_strcmp(tempbuf, req->path + 1) == 0)))
				)
					true = 1;
		}
*/

		if (con->food.source->audiocast.mount[0] == '/') {
			if (	(ice_strcmp(con->food.source->audiocast.mount, req->path) == 0) ||
				(ice_strcmp(tempbuf, req->path) == 0)
			) true = 1;
		} else {
			if (	(ice_strcmp(con->food.source->audiocast.mount, req->path + 1) == 0) ||
				(ice_strcmp(tempbuf, req->path + 1) == 0)
			) true = 1;
		}

		if (true) {
			xa_debug(1, "DEBUG: Found local mount for [%s]", req->path);
			return con;
		}
	}
	return NULL;
}

void
add_chunk (connection_t *con)
{
	int read_bytes;
	int len;
	int tries;

	if (con->food.source->chunk[con->food.source->cid].clients_left > 0)
	{
#ifndef OPTIMIZE
		xa_debug (2, "DEBUG: Kicking trailing clients [%d] on id %d", con->food.source->chunk[con->food.source->cid].clients_left,
			con->food.source->cid);
#endif
		thread_mutex_lock (&info.double_mutex);
		thread_mutex_lock (&con->food.source->mutex);

		kick_clients_on_cid (con->food.source);

		thread_mutex_unlock (&con->food.source->mutex);
		thread_mutex_unlock (&info.double_mutex);
	}

	len = 0;
	read_bytes = 0;
	tries = 0;

	do {
		errno = 0;
#ifdef _WIN32
	        sock_set_blocking(con->sock, SOCK_BLOCK);
#endif

		len = recv(con->sock, con->food.source->chunk[con->food.source->cid].data + read_bytes, SOURCE_READSIZE - read_bytes, 0);

		xa_debug (5, "DEBUG: Source received %d bytes in try %d, total %d, errno: %d", len, tries, read_bytes, errno);

#ifdef _WIN32
		sock_set_blocking(con->sock, SOCK_NONBLOCK);
#endif

		if (con->food.source->connected == SOURCE_KILLED)
			return;

		if ((len == 0) || ((len == -1) && (!is_recoverable(errno)))) {
			if (info.client_timeout > 0 && con->food.source->connected != SOURCE_KILLED) {
				/* Set this source as pending (not connected) */
				pending_connection (con);

				/* Sleep for client_timeout seconds. If during that time this source is set to
				SOURCE_KILLED, return false */

				if (pending_source_signoff (con))
				{
					thread_mutex_lock (&info.double_mutex);
					thread_mutex_lock (&con->food.source->mutex);
					kick_connection (con, "Client timeout exceeded, removing source");
					thread_mutex_unlock (&con->food.source->mutex);
					thread_mutex_unlock (&info.double_mutex);
					return;
				} else {
					thread_mutex_lock (&info.double_mutex);
					thread_mutex_lock (&con->food.source->mutex);
					kick_connection (con, "Lost all clients to new source");
					thread_mutex_unlock (&con->food.source->mutex);
					thread_mutex_unlock (&info.double_mutex);
					return;
				}
			} else {
				thread_mutex_lock (&info.double_mutex);
				thread_mutex_lock (&con->food.source->mutex);
				kick_connection (con, "Source signed off (killed itself)");
				thread_mutex_unlock (&con->food.source->mutex);
				thread_mutex_unlock (&info.double_mutex);
				return;
			}
		} else if (len > 0) {
			read_bytes += len;
			stat_add_read(&con->food.source->stats, len);
			info.hourly_stats.read_bytes += len;
		} else {
			my_sleep(READ_RETRY_DELAY * 1000);
		}

		tries++;
		
	} while ((((double)read_bytes < ((double)SOURCE_READSIZE*3.0)/4.0) && (tries < (READ_TIMEOUT / READ_RETRY_DELAY))));

	if (read_bytes <= 0) {
		android_log(ANDROID_LOG_VERBOSE, "Didn't receive data from source after %d microseconds, assuming it died...", tries * READ_RETRY_DELAY);
		
		/* Set this source as pending (not connected) */
		pending_connection (con);
		
		if (info.client_timeout > 0) {
			/* Sleep for client_timeout seconds. If during that time this source is set to SOURCE_KILLED, return false */
			if (pending_source_signoff(con)) {
				thread_mutex_lock (&info.double_mutex);
				thread_mutex_lock(&con->food.source->mutex);
				kick_connection(con, "Client timeout exceeded, removing source");
				thread_mutex_unlock(&con->food.source->mutex);
				thread_mutex_unlock (&info.double_mutex);
				return;
			} else {
				thread_mutex_lock(&con->food.source->mutex);
				kick_connection(con, "Lost all clients to new source");
				thread_mutex_unlock(&con->food.source->mutex);
				return;
			}
		} else {
			thread_mutex_lock (&info.double_mutex);
			thread_mutex_lock(&con->food.source->mutex);
			kick_connection(con, "Source died");
			thread_mutex_unlock(&con->food.source->mutex);
			thread_mutex_unlock (&info.double_mutex);
			return;
		}
	}

#ifndef OPTIMIZE
	xa_debug (4, "-------add_chunk: Chunk %d was [%d] bytes", con->food.source->cid, read_bytes );
#endif

	con->food.source->chunk[con->food.source->cid].len = read_bytes;
	con->food.source->chunk[con->food.source->cid].clients_left = con->food.source->num_clients;
	con->food.source->cid = (con->food.source->cid + 1) % CHUNKLEN;

}

void
write_chunk(source_t *source, connection_t *clicon)
{
	int i = 0;
	long int write_bytes = 0, len = 0;

	/* Try to write 2 times */
	for (i = 0; i < 2; i++)
	{
		if (source->cid == clicon->food.client->cid) /* No more data available */
			return;

		/* This is how much we should be writing to the client */
		len = source->chunk[clicon->food.client->cid].len - clicon->food.client->offset;

		xa_debug (5, "DEBUG: write_chunk(): Try: %d, writing chunk %d to client %d, len(%d) - offset(%d) == %d", i, clicon->food.client->cid, clicon->id,
			  source->chunk[clicon->food.client->cid].len, clicon->food.client->offset, len);

		if (len < 0 || source->chunk[clicon->food.client->cid].len == 0)
		{
#ifndef OPTIMIZE
			xa_debug (5, "DEBUG: write_chunk: Empty chunk [%d] [%d]", source->chunk[clicon->food.client->cid].len,
				  clicon->food.client->offset );
#endif
			source->chunk[clicon->food.client->cid].clients_left--;
			clicon->food.client->cid = (clicon->food.client->cid + 1) % CHUNKLEN;
			clicon->food.client->offset = 0;
			continue; /* Perhaps for some reason the source read a zero sized chunk but the next one is ok */
		}

		write_bytes = write_data (clicon, source);

		if (write_bytes < 0)
		{
#ifndef OPTIMIZE
			xa_debug (5, "DEBUG: client: [%2d] errors: [%3d]", clicon->id, client_errors (clicon->food.client));
#endif
			if (is_recoverable (0 - write_bytes))
				continue;
			break; /* Safe to assume that the client is kicked out due to socket error */
		}

		clicon->food.client->write_bytes += write_bytes;
		info.hourly_stats.write_bytes += write_bytes;
		stat_add_write (&source->stats, write_bytes);

		if (write_bytes + clicon->food.client->offset >= source->chunk[clicon->food.client->cid].len) {
			source->chunk[clicon->food.client->cid].clients_left--;
			clicon->food.client->cid = (clicon->food.client->cid + 1) % CHUNKLEN;
			clicon->food.client->offset = 0;
		} else {

			clicon->food.client->offset += write_bytes;
#ifndef OPTIMIZE
			xa_debug (5, "DEBUG: client %d only read %d of %d bytes", clicon->id, write_bytes,
				  source->chunk[clicon->food.client->cid].len - clicon->food.client->offset);
#endif
		}
	}

	xa_debug (4, "DEBUG: client %d tried %d times, now has %d errors %d chunks behind source", clicon->id, i,
		  client_errors (clicon->food.client), source->cid < clicon->food.client->cid ? source->cid+CHUNKLEN - clicon->food.client->cid : source->cid - clicon->food.client->cid);

	if (clicon->food.client->alive == CLIENT_DEAD)
		return;
}

void
kick_clients_on_cid(source_t *source)
{
	avl_traverser trav = {0};
	connection_t *clicon;

	unsigned int max = avl_count (source->clients) * avl_count (source->clients) + 2;

	xa_debug (3, "Clearing cid %d", source->cid);

	zero_trav (&trav);

#if !defined(SAVE_CPU) || !defined(OPTIMIZE)
	xa_debug (5, "DEBUG: In function kick_clients_on_cid. Source has %d clients", max);
#endif
	while (max >= 0)
	{
		clicon = avl_traverse (source->clients, &trav);
		if (!clicon)
			break;

		if (client_errors (clicon->food.client) >= (CHUNKLEN - 1) && clicon->food.client->alive != CLIENT_DEAD)

		{
			kick_connection (clicon, "Client cannot sustain sufficient bandwidth");
			zero_trav (&trav); /* Start from the top of the tree */
		}
		max--;
	}
	source->chunk[source->cid].clients_left = 0;
#if !defined(SAVE_CPU) || !defined(OPTIMIZE)
	xa_debug (5, "DEBUG: leaving function kick_clients_on_cid");
#endif
}

/*
 * Can't be removing clients inside the loop which handles all the
 * write_chunk()s, instead we kick all the dead ones for each chunk.
 */
void
kick_dead_clients(source_t *source)
{
	avl_traverser trav = {0};
	connection_t *clicon = NULL;

	int max = avl_count (source->clients) * avl_count (source->clients) + 2;

#if !defined(SAVE_CPU) || !defined(OPTIMIZE)
	xa_debug (5, "DEBUG: In function kick_dead_clients. Will run %d laps", max);
#endif

	/* Check for too many errors */
	while ((clicon = avl_traverse (source->clients, &trav))) {
		if (client_errors (clicon->food.client) >= (CHUNKLEN - 1)) {
				kick_connection (clicon, "Too many errors (client not receiving data fast enough)");
		}
	}

	zero_trav (&trav);

	while (max >= 0)
	{
		clicon = avl_traverse (source->clients, &trav);
		if (!clicon)
			break;

		if (clicon->food.client->alive == CLIENT_MOVE) {
				kick_connection (clicon, "Smaller source stream signed off");
			/* No zero_trav() here cause it's not removed from the list (close_connection() does that) */
		}

		if (clicon->food.client->alive == CLIENT_DEAD) {
			close_connection (clicon, &info);
			zero_trav (&trav);
		}

		max--;
	}

#if !defined(SAVE_CPU) || !defined(OPTIMIZE)
	xa_debug (5, "DEBUG: leaving function kick_dead_clients, %d laps left", max);
#endif
}

int
write_data (connection_t *clicon, source_t *source)
{
	int write_bytes;

	if (source->chunk[clicon->food.client->cid].len - clicon->food.client->offset <= 0)
		return 0;

	write_bytes = sock_write_bytes_or_kick(clicon->sock, clicon, &source->chunk[clicon->food.client->cid].data[clicon->food.client->offset],
					       source->chunk[clicon->food.client->cid].len - clicon->food.client->offset);

#ifndef OPTIMIZE
	xa_debug (4, "DEBUG: client %d in write_data(). Function write() returned %d of %d bytes, client on chunk %d (+%d), source on chunk %d", clicon->id, write_bytes,
		  source->chunk[clicon->food.client->cid].len - clicon->food.client->offset, clicon->food.client->cid, clicon->food.client->offset,
		  source->cid);
#endif
	if (write_bytes < 0)
		return 0 - errno;
	return write_bytes;
}

const char source_protos[2][12] = { "icy", "x-audiocast" };
const char source_types[5][16] = { "encoder", "pulling relay", "on demand relay", "file transfer", "unknown source" };

const char *
sourcetype_to_string (source_type_t type)
{
	return source_types[type];
}

/* What we want to do here is give the client the best possible
 * chunk in the source to start from. Where it suffers the least
 * from both his own slow network connection and discrepanices
 * in the source feed.
 */
int
start_chunk (source_t *source)
{
	return source->cid > 0 ? source->cid - 1 : CHUNKLEN - 1;
}

void
source_write_to_client (source_t *source, connection_t *clicon)
{
	client_t *client;

	if (!clicon || !source) {
		xa_debug (1, "WARNING: source_write_to_client() called with NULL pointers");
		return;
	}

	client = clicon->food.client;

	if (client->alive == CLIENT_DEAD)
		return;

	if (client->virgin == CLIENT_PAUSED || client->virgin == -1)
		return;

	if (client->virgin == CLIENT_UNPAUSED)	{
		client->cid = start_chunk (source);
		client->offset = find_frame_ofs (source);
		client->virgin = 0;
	}

	if (client->virgin == 1) {
		client->cid = start_chunk (source);
		client->offset = find_frame_ofs(source);
		xa_debug (2, "Client got offset %d", client->offset);
		client->virgin = 0;
		source->num_clients = source->num_clients + (unsigned long int)1;
	}

  write_chunk (source, clicon);
}

void
source_get_new_clients (source_t *source)
{
	connection_t *clicon;

	while ((clicon = pool_get_my_clients (source))) {
		xa_debug (1, "DEBUG: source_get_new_clients(): Accepted client %d", clicon->id);
		avl_insert (source->clients, clicon);
	}
}

