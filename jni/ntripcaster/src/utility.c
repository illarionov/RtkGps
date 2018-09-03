/* utility.c
 * - General Utility Functions
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

#ifdef HAVE_ASSERT_H
#include <assert.h>
#endif
#include <string.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#include <stdlib.h>
#include <sys/types.h>
#include <fcntl.h>

#ifndef _WIN32
# include <netdb.h>
# include <sys/socket.h>
# include <netinet/in.h>
# include <arpa/inet.h>
# include <sys/stat.h>
# include <time.h>
# include <errno.h>
#else
# include <winsock.h>
# include <io.h>
# define access _access
# define open _open
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "utility.h"
#include "ntrip_string.h"
#include "sock.h"
#include "source.h"
#include "client.h"
#include "log.h"
#include "main.h"
#include "timer.h"
#include "string.h"
#include "connection.h"


extern server_info_t info;
extern int running;

int password_match(const char *crypted, const char *uncrypted)
{
#ifndef USE_CRYPT
	if (!crypted || !uncrypted)	{
		android_log(ANDROID_LOG_VERBOSE, "ERROR: password_match called with NULL arguments");
		return 0;
	}

	if (ice_strcmp(crypted, uncrypted) == 0)
		return 1;

	return 0;
#else
	char salt[3];
	char *test_crypted;
	extern char *crypt(const char *, const char *);

	if (!crypted || !uncrypted)	{
		android_log(ANDROID_LOG_VERBOSE, "ERROR: password_match called with NULL arguments");
		return 0;
	}

	if (ice_strncmp(crypted, "$1$", 3)) {
		salt[0] = crypted[0];
		salt[1] = crypted[1];
	} else {
		salt[0] = crypted[3];
		salt[1] = crypted[4];
	}

	salt[2] = '\0';

	thread_mutex_lock(&info.misc_mutex);
	test_crypted = crypt(uncrypted, salt);
	if (test_crypted == NULL) {
		android_log(ANDROID_LOG_VERBOSE, "WARNING - crypt() failed, refusing access");
		thread_mutex_unlock(&info.misc_mutex);
		return 0;
	}
	if (ice_strcmp(test_crypted, crypted) == 0)	{
		thread_mutex_unlock(&info.misc_mutex);
		return 1;
	}
	
	thread_mutex_unlock(&info.misc_mutex);

	return 0;
#endif
}

int find_frame_ofs(source_t *source)
{
	char *buff;
	int pos = 0, cid;
	
	if (!source)
	{
		android_log (ANDROID_LOG_VERBOSE, "ERROR: find_frame_ofs() called with NULL argument");
		return 0;
	}

	cid = source->cid <= 0 ? CHUNKLEN - 1 : source->cid - 1;
	
	buff = source->chunk[cid].data;
	
	while (pos < source->chunk[cid].len - 1) {
		if ((buff[pos] & 0xFF) == 0xFF && (buff[pos + 1] & 0xF0) == 0xF0)
			break;
		pos++;
	}
	
	return pos;
}
		
void
kick_connection(void *conarg, void *reasonarg)
{
	connection_t *con = (connection_t *)conarg;
	char *reason = (char *)reasonarg;

	char timebuf[BUFSIZE] = "";

	if (!conarg || !reasonarg)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: kick_connection called with NULL pointers");
		return;
	}
	
	switch (con->type)
	{
		case client_e:
			android_log (ANDROID_LOG_VERBOSE,
				   "Kicking client %lu [%s] [%s] [%s] [%s], connected for %s, %lu bytes transfered. %lu clients connected",
				   con->id, nullcheck_string(con->user), con_host (con), reason, con->food.client->type == listener_e ? "listener" : "relay",
				   nice_time (get_time () - con->connect_time, timebuf), con->food.client->write_bytes, info.num_clients - 1);
			con->food.client->alive = CLIENT_DEAD;

			return;
			break;

		case source_e:
			android_log (ANDROID_LOG_VERBOSE,
				   "Kicking source %lu [%s] [%s] [%s], connected for %s, %lu bytes transfered. %lu sources connected",
				   con->id, con_host (con), reason, con->food.source->type == encoder_e ? "encoder" : "relay",
				   nice_time (get_time () - con->connect_time, timebuf), con->food.source->stats.read_bytes, info.num_sources - 1);
			if (con->food.source->connected == SOURCE_UNUSED)
				close_connection (con, NULL);
			else
			{
				sock_close(con->sock);
				con->food.source->connected = SOURCE_KILLED;
			}

			return;
			break;
		default:
			android_log(ANDROID_LOG_VERBOSE, "Kicking unknown %lu [%s] [%s], connected for %s", con->id, con_host (con), reason,
				  nice_time (get_time () - con->connect_time, timebuf));
			break;
	}
	close_connection(con, &info);
	return;
}

void
kick_everything ()
{
  connection_t *con, *con2;
  android_log(ANDROID_LOG_VERBOSE, "Kicking everything...");
  while ((con = avl_get_any_node (info.sources)))
  {
	  while ((con2 = avl_get_any_node (con->food.source->clients)))
		  kick_connection (con2, "Masskick by admin");
	  kick_connection (con, "Masskick by admin");
  }

  return;
}

void
free_con (connection_t *con)
{
	if (con->sock >= 0)
	{
		if (!(con->host && ice_strcmp (con->host, "NtripCaster console") == 0)) {	
			sock_close (con->sock);
			con->sock = -1;
		}
	}

	if (con->host != NULL)
	{
		nfree(con->host);
		con->host = NULL;
	}

	if (con->headervars != NULL)
		free_con_variables (con);
		
	if (con->sin != NULL)
		nfree (con->sin);
	
	if (con->hostname != NULL)
	{
		nfree(con->hostname);
		con->hostname = NULL;
	}

	if (con->user != NULL)
		nfree (con->user);
}

/* Must have the mutex when calling this:
   client: must have the current source mutex
   source: must have its own mutex, and info.source_mutex */
void 
close_connection(void *data, void *param)
{
	connection_t *con = (connection_t *)data;
	
	if (!con) {
		android_log(ANDROID_LOG_VERBOSE, "ERROR: close_connection called with null pointer!");
		return;
	}
	
	xa_debug (2, "DEBUG: Removing connection %d of type %d", con->id, con->type);

	free_con (con);

	if (con->type == client_e) {
		source_t *con2 = source_with_client(con);

		if (!con2)
			xa_debug (2, "DEBUG: Client with NULL source?");

		if (con->food.client->virgin == 0)
			del_client (con, con2);
		else
			util_decrease_total_clients ();

		if (con2)
		  {
			  con2->stats.client_connect_time += (unsigned long)((get_time () - con->connect_time) / 60.0);
			  info.hourly_stats.client_connect_time += (unsigned long)((get_time() - con->connect_time) / 60.0);

			  xa_debug (2, "DEBUG: Removing client %d (%p) from sourcetree of (%p)", con->id, con, con2);

			  if (!avl_delete(con2->clients, con))
				  xa_debug (2, "DEBUG: Didn't find client in sourcetree!");
		  } else {
			  xa_debug (2, "DEBUG: client %d without source?", con->id);
		  }
		nfree (con->food.client);
		nfree (con);
		return;
	}

	else if (con->type == source_e) {
		source_t *source = con->food.source;
		connection_t *clicon;

		if (!source)
		{
			android_log (ANDROID_LOG_VERBOSE, "WARNING!!! - Erroneous source without food");
			return;
		}

		xa_debug (2, "Removing source %d (%p) from sourcetree of (%p)", con->id, con, info.sources);

		if (source->clients != NULL)
		{
			avl_traverser trav = {0};
			android_log(ANDROID_LOG_VERBOSE, "Kicking all %lu clients for source %lu",
				  source->num_clients, con->id);
			while ((clicon = avl_traverse (source->clients, &trav)))
			{
					kick_connection (clicon, "Stream ended");
			}

			kick_dead_clients (source);
			avl_destroy (source->clients, NULL);
		}

		dispose_audiocast (&source->audiocast);

		info.hourly_stats.source_connect_time += ((get_time () - con->connect_time) / 60);

		if (con->food.source->connected != SOURCE_UNUSED)
		{
			del_source();
			avl_delete (info.sources, con);
		}

		if (source->source_agent != NULL) nfree(source->source_agent);

		if (source->mutex.thread_id >= 0)
			thread_mutex_unlock (&source->mutex);
		thread_mutex_destroy (&source->mutex);

		nfree(source);
		nfree(con);

		return;
	}

	nfree(con); /* Unknown connection */
	return;
}

void
kick_not_connected (connection_t *con, char *reason)
{
	char timebuf[BUFSIZE];
	char typebuf[10];

	if (reason)
		android_log (ANDROID_LOG_VERBOSE, "Kicking %s %lu [%s] [%s], connected for %s", type_of_str (con->type, typebuf), con->id, con_host (con), reason,
			   nice_time (get_time () - con->connect_time, timebuf));
	
	free_con (con);

	if (con->type == source_e)
	  {
		  avl_destroy (con->food.source->clients, NULL);
		  nfree (con->food.source);
	  }
	else if (con->type == client_e)
	  {
	    nfree (con->food.client);
	  }
	nfree (con);
}

source_t *
source_with_client(connection_t *con)
{
	if (con)
		return (con->food.client->source);
	return NULL;
}  

#ifndef _WIN32
int server_detach()
{
	pid_t icepid;

	android_log(ANDROID_LOG_VERBOSE, "Trying to fork");

	icepid = fork();
	if (icepid == -1) {
		android_log(ANDROID_LOG_VERBOSE, "ERROR: Can't fork dammit!");
		return 1;
	}
  
	if (icepid != 0) {
#if HAVE_SETPGID
		android_log(ANDROID_LOG_VERBOSE, "Detached (pid: %d)", icepid);
		setpgid(icepid, icepid);
#endif
		exit(0);
	} else {
#if HAVE_SETPGID
		setpgid(0, 0);
#endif
		freopen("/dev/null", "r", stdin);
		freopen("/dev/null", "w", stdout);
		freopen("/dev/null", "w", stderr);
		fd_close(0);
		fd_close(1);
		fd_close(2);
	}
	return 1;
}
#endif

connection_t *
find_source_with_mount (char *mount)
{
	avl_traverser trav = {0};
	connection_t *scon = NULL;

	thread_mutex_lock (&info.source_mutex);
	
	while ((scon = avl_traverse (info.sources, &trav))) {
		if (ice_strcmp (mount, scon->food.source->audiocast.mount) == 0)
			break;
	}
	
	thread_mutex_unlock (&info.source_mutex);
	
	return scon;
}

unsigned long int
new_id ()
{
	unsigned long int ret;
	thread_mutex_lock (&info.misc_mutex);
	ret = info.id;
	info.id++;
	thread_mutex_unlock (&info.misc_mutex);
	return ret;
}

void
kill_threads ()
{
	avl_traverser trav = {0};
	connection_t *con;
	mythread_t *mt;

	android_log (ANDROID_LOG_VERBOSE, "Telling threads to die...");

	while ((mt = avl_traverse (info.threads, &trav))) {
		mt->running = THREAD_KILLED;
	}
	
	android_log (ANDROID_LOG_VERBOSE, "Closing sockets for sources that keep hanging around...");
	
	zero_trav (&trav);

	thread_mutex_lock (&info.source_mutex);
	while ((con = avl_traverse (info.sources, &trav))) {
		sock_close(con->sock);
		con->food.source->connected = SOURCE_KILLED;
	}
	
	thread_mutex_unlock (&info.source_mutex);
}

time_t
tree_time(avl_tree *tree)
{
	connection_t *con;
	avl_traverser trav = {0};
	time_t t, tc = 0;
	
	t = get_time();
	while ((con = avl_traverse(tree, &trav)))
		tc += (t - con->connect_time);
	return tc / 60;
}
		
void 
write_icecast_header ()
{
	printf("NtripCaster Version %s Initializing...\n", info.version);
	printf("NtripCaster comes with NO WARRANTY, to the extent permitted by law.\nYou may redistribute copies of NtripCaster under the terms of the\nGNU General Public License.\nFor more information about these matters, see the file named COPYING.\n");
	printf("Starting thread engine...\n");
	android_log(ANDROID_LOG_VERBOSE, "NtripCaster Version %s Starting..", info.version);
}

void
print_startup_server_info ()
{
	int i;

	if (info.myhostname && ice_strcmp (info.myhostname, "0.0.0.0"))
		android_log(ANDROID_LOG_VERBOSE, "Listening on host %s...", info.myhostname);

	for (i = 0; i < MAXLISTEN; i++) {
		if (info.port[i] > 0)
			android_log(ANDROID_LOG_VERBOSE, "Listening on port %i...", info.port[i]);
	}

	if (info.server_name)
		android_log (ANDROID_LOG_VERBOSE, "Using '%s' as servername...", info.server_name);

	android_log(ANDROID_LOG_VERBOSE, "Server limits: %lu clients, %lu clients per source, %lu sources",
		  info.max_clients, info.max_clients_per_source, info.max_sources);
}

char *
connect_average (unsigned long int minutes, unsigned long int connections, char *buf)
{
	if (!connections)
		return nice_time_minutes (minutes, buf);
	return nice_time_minutes ((unsigned long int)((double) minutes  / (double) connections), buf);
}

int
hostname_local (char *name)
{
	char *new;

	if (!name)
	{
		android_log (ANDROID_LOG_VERBOSE, "ERROR: hostname_local called with NULL name");
		return 0;
	}

	if (!name[0])
		return 1;

	if (ice_strcasecmp (name, "localhost") == 0 || ice_strcasecmp (name, "127.0.0.1") == 0)
		return 1;
	if (info.server_name && ice_strcasecmp (name, info.server_name) == 0)
		return 1;
	if (info.myhostname && ice_strcasecmp (name, info.myhostname) == 0)
		return 1;

	/* Search the tree */
	thread_mutex_lock (&info.hostname_mutex);
	
	new = avl_find (info.my_hostnames, name);

	thread_mutex_unlock (&info.hostname_mutex);

	if (new)
		return 1;

	/* Not in the tree, try to reverse it */
	{
		char buf[BUFSIZE], *out;
		char *res = forward (name, buf);

		if (!res)
			return 0; /* Unresolvable */
			
		thread_mutex_lock (&info.hostname_mutex);

		out = avl_find (info.my_hostnames, res);
		
		thread_mutex_unlock (&info.hostname_mutex);

		if (out || (info.myhostname && (ice_strcasecmp (res, info.myhostname) == 0)) || (ice_strcmp (res, "127.0.0.1") == 0))
		{
			thread_mutex_lock (&info.hostname_mutex);
			avl_insert (info.my_hostnames, nstrdup (name));
			thread_mutex_unlock (&info.hostname_mutex);
			return 1;
		}
	}
	return 0;
}

void
build_request (char *line, request_t *req)
{
	char path[BUFSIZE] = "";
	char *ptr, *lineptr;

	if (!line || !req)
	{
		android_log (ANDROID_LOG_VERBOSE, "ERROR: build_request called with NULL pointer");
		return;
	}

	xa_debug (2, "DEBUG: Building request out of [%s]", line);

	if (ice_strncmp (line, "GET", 3) == 0)
	{

		splitc (NULL, line, ' ');

		/* line consists of either
		   "/ HTTP/1.0"
		   "HTTP/1.0"
		   "http://somewhat.com/ HTTP/1.0"
		   "http://somewhat.com:8000/ HTTP/1.0"
		   "http://somewhat.com:8000 HTTP/1.0"
		*/

		if ((splitc (path, line, ' ') == NULL) || !path[0] || path[0] == ' ')
		{
			xa_debug (1, "Empty GET request [%s]", line);
			strncpy (req->path, "/", BUFSIZE);
			
			if (info.server_name)
				strncpy (req->host, info.server_name, BUFSIZE);
			else
				strncpy (req->host, "localhost", BUFSIZE);
			req->port = info.port[0];
			return;
		}

		if (ice_strncmp (path, "http://", 7) == 0)
		{
			lineptr = &path[7];

			xa_debug (2, "DEBUG: Building http request from [%s]", lineptr);

			ptr = strchr (lineptr, ':');
			
			if (ptr) /* port present */
			{
				splitc (req->host, lineptr, ':');
				req->port = atoi (lineptr);

				lineptr = strchr (lineptr, '/');
				
				if (!lineptr)
					strncpy (req->path, "/", BUFSIZE);
				else
				{
					if (splitc (req->path, lineptr, ' ') == NULL)
						strncpy (req->path, lineptr, BUFSIZE);
				}
				return;
			} else {
				req->port = info.port[0];

				ptr = strchr (lineptr, '/');

				if (!ptr)
				{
					if (splitc (req->host, lineptr, ' ') == NULL)
						strncpy (req->host, lineptr, BUFSIZE);
					strncpy (req->path, "/", BUFSIZE);
					return;
				} else {
					if (splitc (req->host, lineptr, '/') == NULL)
						strncpy (req->host, lineptr, BUFSIZE);
					else if (splitc (req->path, lineptr, ' ') == NULL)
						strncpy (req->path, lineptr, BUFSIZE);

					if (req->path[0] != '/')
						strncpy (req->path, "/", BUFSIZE);
					return;
				}
			}

		} else {
			char pathbuf[BUFSIZE];

			xa_debug (2, "DEBUG: Building clean request from [%s]", path);

			req->port = info.port[0];
			
			if (splitc (pathbuf, path, ' ') == NULL)
				strncpy (pathbuf, path, BUFSIZE);

			if (pathbuf[0] == '/')
				strncpy (req->path, pathbuf, BUFSIZE);
			else
				snprintf (req->path, BUFSIZE, "/%s", pathbuf);

			if (info.server_name)
				strncpy (req->host, info.server_name, BUFSIZE);
			else
				strncpy (req->host, "localhost", BUFSIZE);
			return;
		}
		
	} else if ((ice_strncmp (line, "HOST:", 5) == 0) || (ice_strncmp (line, "Host:", 5) == 0))
	{
		char hostbuf[BUFSIZE] = "";

		splitc (NULL, line, ':');

		while (*line == ' ')
			line++;

		ptr = strchr (line, ':');

		if (ptr)
		{
			splitc (hostbuf, line, ':');
			req->port = atoi (line);
		} else {
		    if (splitc (hostbuf, line, ' ') == NULL) strncpy (hostbuf, line, BUFSIZE);
		}
		
		strncpy (req->host, hostbuf, BUFSIZE);

		return;
	} else {
		xa_debug (1, "DEBUG: Build request called with invalid line [%s]", line);
	}
}
		
int
mount_exists (char *mount)
{
	avl_traverser trav = {0};
	connection_t *scon;

	thread_mutex_lock (&info.source_mutex);

	while ((scon = avl_traverse (info.sources, &trav)))
	{
		if ((ice_strcmp (mount, scon->food.source->audiocast.mount) == 0) && (scon->food.source->connected != SOURCE_PENDING))
		{
			thread_mutex_unlock (&info.source_mutex);
			return 1;
		}
	}

	thread_mutex_unlock (&info.source_mutex);

	return 0;
}

void
generate_http_request (char *line, request_t *req)
{
	char full[BUFSIZE + 50] = "";
	
	if (!line || !req)
	{
		android_log (ANDROID_LOG_VERBOSE, "ERROR: generate_request called with NULL pointer");
		return;
	}

//	if (ice_strncmp (line, "http://", 7) == 0)
//	{
		snprintf (full, BUFSIZE, "GET %s HTTP/1.0", line);

//	} else {
//		snprintf (full, BUFSIZE, "GET http://%s HTTP/1.0", line);
//	}

	build_request (full, req);

	xa_debug (2, "DEBUG: Generated http request [%s:%d%s]", req->host, req->port, req->path);
}

void
zero_request (request_t *req)
{
	req->host[0] = '\0';
	req->path[0] = '\0';
	req->user[0] = '\0';
	req->port = -1;
}

void init_thread_tree(int line, char *file)
{
	mythread_t *mt;
	
	if (!file) {
		fprintf (stderr, "WARNING: init_thread_tree() called with file == NULL\n");
		exit (1);
	}

	info.threads = NULL;
	info.threadid = 0;

#ifdef DEBUG_MEMORY
	info.mem = avl_create_nl(compare_mem, &info);
	thread_create_mutex (&info.memory_mutex);
#endif

	mt  = (mythread_t *)nmalloc(sizeof(mythread_t));

	/* Create a tree for all threads */
	info.threads = avl_create(compare_threads, &info);

	mt->id = 0;
	mt->line = line;
	mt->file = strdup(file);
	mt->thread = thread_self();
	mt->created = get_time();
	mt->name = strdup("Main Thread");

	if (avl_insert(info.threads, mt)) {
		android_log (ANDROID_LOG_VERBOSE, "WARNING: Could not insert main thread into the thread tree, DAMN!\n");
		exit(1);
	}
	
	thread_create_mutex(&info.thread_mutex);

	/* On platforms where it is supported, this enables this thread to be
	   cancelable */
	thread_init();

	thread_catch_signals ();

	thread_setup_default_attributes();
}

void
pending_connection (connection_t *con)
{
	android_log (ANDROID_LOG_VERBOSE, "Lost connection to source on mount %s, waiting %d seconds for timeout", con->food.source->audiocast.mount, info.client_timeout);
	con->food.source->connected = SOURCE_PENDING;

}

int
pending_source_signoff (connection_t *con)
{
	time_t start = get_time ();
	while ((running == SERVER_RUNNING) && con->food.source->connected == SOURCE_PENDING && ((get_time () - start) < info.client_timeout))
		my_sleep(90000);
	if (con->food.source->connected == SOURCE_PENDING)
		return 1;
	return 0;
}

int open_for_reading(const char *filename)
{
	int fd;

	if (!filename) {
		xa_debug(1, "ERROR: Cannot open file for reading no file specified");
		return -1;
	}

	fd = open(filename, O_RDONLY);
	if (fd == -1)
		xa_debug(1, "ERROR: Cannot open file for reading [%s]", filename);

	return fd;
}

int
open_for_append (const char *filename)
{
  int fd;

  if (!filename)
	  return -1;

  fd = open (filename, O_WRONLY|O_APPEND|O_CREAT, 00644);
  if (fd == -1)
    xa_debug (1, "ERROR: Cannot open file for append [%s]", filename);
  return fd;
}

char *
get_log_file (const char *filename) 
{
	char logdir[BUFSIZE];

	if (!filename || !info.logdir) {
		fprintf (stderr, "WARNING: get_log_file() called with NULLs\n");
		return NULL;
	}

	snprintf (logdir, BUFSIZE, "%s%c", info.logdir, DIR_DELIMITER);
	xa_debug (1, "DEBUG: Checking directory %s", logdir);

	if (access (info.logdir, R_OK) == 0) {
		return ice_cat (logdir, filename);
	}

	snprintf (logdir, BUFSIZE, "log%c", DIR_DELIMITER);
	xa_debug (1, "DEBUG: Checking directory %s", logdir);
	if (access (logdir, R_OK) == 0) {
		return ice_cat (logdir, filename);
	}

	snprintf (logdir, BUFSIZE, "%s%c", ".", DIR_DELIMITER);
	if (access (logdir, R_OK) == 0) {
		return ice_cat (logdir, filename);
	}

	return NULL;
}

char *
get_icecast_file(const char *filename, filetype_t type, int flags)
{
	char path_and_file [BUFSIZE];

	if (!filename || !info.etcdir || !info.logdir) {
		xa_debug(1, "ERROR: get_icecast_file(): called with NULL pointer");
		return NULL;
	}

	path_and_file[0] = '\0';

	switch (type) {
		case conf_file_e:
			snprintf(path_and_file, BUFSIZE, "%s%c%s", info.etcdir, DIR_DELIMITER, filename);
			break;
		case log_file_e:
			snprintf(path_and_file, BUFSIZE, "%s%c%s", info.logdir, DIR_DELIMITER, filename);
			break;
		default:
			snprintf(path_and_file, BUFSIZE, "%s", filename);
	}

	xa_debug(3, "DEBUG: get_icecast_file(): Looking for %s", path_and_file);

	if (access(path_and_file, flags) == 0)
		return nstrdup(path_and_file);
	
	switch (type) {
		case conf_file_e:
			snprintf(path_and_file, BUFSIZE, ".%c%s%c%s", DIR_DELIMITER, "conf", DIR_DELIMITER, filename);
			break;
		case log_file_e:
			snprintf(path_and_file, BUFSIZE, ".%c%s%c%s", DIR_DELIMITER, "logs", DIR_DELIMITER, filename);
			break;
		default:
			snprintf(path_and_file, BUFSIZE, "%s", filename);
	}
	
	xa_debug(3, "DEBUG: get_icecast_file(): Looking for %s", path_and_file);

	if (access(path_and_file, flags) == 0)
		return nstrdup(path_and_file);

	switch (type) {
		case conf_file_e:
			snprintf(path_and_file, BUFSIZE, "..%c%s%c%s", DIR_DELIMITER, "conf", DIR_DELIMITER, filename);
			break;
		case log_file_e:
			snprintf(path_and_file, BUFSIZE, "..%c%s%c%s", DIR_DELIMITER, "logs", DIR_DELIMITER, filename);
			break;
		default:
			snprintf(path_and_file, BUFSIZE, "%s", filename);
	}
	
	xa_debug(3, "DEBUG: get_icecast_file(): Looking for %s", path_and_file);

	if (access(path_and_file, flags) == 0)
		return nstrdup(path_and_file);

	xa_debug (2, "DEBUG: get_icecast_file(): Didn't find %s", filename);

	return NULL;
}

#define KILO (32*32)
void
stat_add_read (statistics_t *stat, int len)
{
	while (stat->read_bytes + len >= KILO)
	{
		stat->read_kilos++;
		len -= KILO;
	}

	stat->read_bytes += len;
}

void
stat_add_write (statistics_t *stat, int len)
{
	while (stat->write_bytes + len >= KILO)
	{
		stat->write_kilos++;
		len -= KILO;
	}

	stat->write_bytes += len;
}

char *
type_of_str (contype_t type, char *buf)
{
	if (type == client_e)
		sprintf (buf, "client");
	else if (type == source_e)
		sprintf (buf, "source");
	else
		sprintf (buf, "unknown");

	return buf;
}

void
my_sleep (int microseconds)
{
#ifdef _WIN32
	Sleep (microseconds/1000);
#else
# ifdef HAVE_NANOSLEEP
        struct timespec req, rem;
	long nanoseconds;

	req.tv_sec = 0;
	req.tv_nsec = 0;
	
	while (microseconds > 999999) {
		req.tv_sec++;
		microseconds -= 1000000;
	}

	nanoseconds = microseconds * 1000;
 
        while (nanoseconds > 999999999)
        {
                req.tv_sec++;
                nanoseconds -= 1000000000;
        }

        req.tv_nsec = nanoseconds;

        switch (nanosleep (&req, &rem)) {
		case EINTR:
			xa_debug (4, "WARNING: nanosleep() was interupted by nonblocked signal");
			break;
		case EINVAL:
			xa_debug (1, "WARNING: nanosleep() was passed invalid or negative sleep value %ld+%ld",
				  req.tv_sec, req.tv_nsec);
			break;
	}

# elif HAVE_SELECT
        struct timeval sleeper;
        sleeper.tv_sec = 0;
        sleeper.tv_usec = microseconds;
        select (1, NULL, NULL, NULL, &sleeper);
# else
        usleep (microseconds);
# endif
#endif
}

int
is_recoverable (int error)
{
#ifdef _WIN32
	if ((WSAGetLastError() == WSAEWOULDBLOCK) || (WSAGetLastError() == WSAEINTR) || (WSAGetLastError() == WSAEINPROGRESS)) 
		return 1;
#else
	if ((error == EAGAIN) || (error == EINTR) || (error == EINPROGRESS))
		return 1;
#endif

#ifdef SOLARIS
	if ((error == EWOULDBLOCK))
	  return 1;
#endif

#ifdef LINUX
	if (error == EIO) /* Works around an error with gdb and linux */
		return 1;
#endif

	return 0;
}

void
set_run_path (char *argv)
{
	char *pos;
	int i;

	info.runpath = strdup (argv);
	running = SERVER_INITIALIZING;
	pos = strrchr (info.runpath, DIR_DELIMITER);
	if (pos) {
		*(pos + 1) = '\0';
		i = strlen(info.runpath) - 1;
		if ((i >= 3) && 
		    (info.runpath[i-1] == 'n' || info.runpath[i-1] == 'N') &&
		    (info.runpath[i-2] == 'i' || info.runpath[i-3] == 'I') &&
		    (info.runpath[i-3] == 'b' || info.runpath[i-4] == 'B')) {
			info.runpath[i-3] = '\0';
		}
	}
	android_log(ANDROID_LOG_VERBOSE,"runpath: %s",info.runpath);
}

void
dispose_audiocast (audiocast_t *au)
{
	if (!au)
		return;
	nfree (au->name);
	nfree (au->mount);
}

int
count_clients() {

	connection_t *clicon, *sourcecon;
	avl_traverser trav = {0}, sourcetrav = {0};
	int num = 0;

	thread_mutex_lock (&info.double_mutex);
	thread_mutex_lock (&info.source_mutex);

	while ((sourcecon = avl_traverse (info.sources, &sourcetrav)))
	{
		zero_trav (&trav);
		thread_mutex_lock (&sourcecon->food.source->mutex);

		while ((clicon = avl_traverse (sourcecon->food.source->clients, &trav))) num++;
	
		thread_mutex_unlock (&sourcecon->food.source->mutex);
	}

	thread_mutex_unlock (&info.source_mutex);
	thread_mutex_unlock (&info.double_mutex);

	return num;
}

/*
 * Lets the thread sleep a random amount of time (maximal max seconds)
 */
void
sleep_random(int max) {

	my_sleep(((rand()%(max*1000))+1)*1000);
}

/* the settings for the config file */
set_element configfile_settings[] =
{
	{ "encoder_password", string_e,  "The encoder password", NULL },
	{ "max_clients", integer_e,      "Highest number of client connectons",  NULL },
	{ "max_sources", integer_e,      "Highest number of source connections", NULL },
	{ "logfile", string_e,           "Logfile to write to", NULL },
	{ "server_name", string_e,	 "Server's hostname", NULL},
	{ "max_clients_per_source", integer_e, "Max number of clients listening on one source", NULL},
	{ "location", string_e, "NtripCaster server geographical location", NULL},
	{ "rp_email", string_e, "Resposible person email", NULL},
  { "server_url", string_e, "URL for this NtripCaster server", NULL},
	{ "logdir", string_e, "Directory for log files", NULL},
	{ (char *) NULL, 0, (char *) NULL, NULL }
};

void
setup_config_file_settings()
{
	int x = 0;
	/* Don't change the order of these */
	configfile_settings[x++].setting = &info.encoder_pass;
	configfile_settings[x++].setting = &info.max_clients;
	configfile_settings[x++].setting = &info.max_sources;
	configfile_settings[x++].setting = &info.logfilename;
	configfile_settings[x++].setting = &info.server_name;
	configfile_settings[x++].setting = &info.max_clients_per_source;
	configfile_settings[x++].setting = &info.location;
	configfile_settings[x++].setting = &info.rp_email;
	configfile_settings[x++].setting = &info.server_url;
	configfile_settings[x++].setting = &info.logdir;
}

set_element *
find_set_element (char *name, set_element *el)
{
  register int i;
  for (i = 0; el[i].name; i++)
    if (ice_strcmp (name, el[i].name) == 0)
      return (&el[i]);
  return ((set_element *)NULL);
}

int
parse_default_config_file()
{
	char *file = get_icecast_file(info.configfile, conf_file_e, R_OK);
	
	if (file) {
		parse_config_file(file);
		nfree(file);
		return 1;
	}
	
	parse_config_file(info.configfile);
	return 1;
}
		
int
parse_config_file(char *file)
{
	set_element *se;
	char word[BUFSIZE], line[BUFSIZE];
	int cf;
	int i;
	int lineno = 0;

	xa_debug (1, "DEBUG: Parsing configuration file %s", file ? file : "(null)");

	if (!file)
		return 0;

	if ((cf = open_for_reading (file)) == -1)
	{
		android_log(ANDROID_LOG_VERBOSE, "No configfile found, using defaults.");
		return 1;
	}

	for (i = 0; i < MAXLISTEN; i++) {
		info.port[i] = 0;
	}

	while (fd_read_line (cf, line, BUFSIZE) > 0)
	{
		lineno++;
		if ((ice_strlen (line) < 2) || (line[0] == '#') || (line[0] == ' '))
			continue;
		
		if (line[ice_strlen(line) - 1] == '\n')
			line[ice_strlen(line) - 1] = '\0';

		if (line[0] == '/') {
			add_authentication_mount(create_mount_from_line(line));
      continue;
		}

		if (splitc(word, line, ' ') == NULL) {
			android_log(ANDROID_LOG_VERBOSE, "ERROR: No argument given to setting %s on line %d",
				   line, lineno);
			continue;
		}
		
		se = find_set_element(word, configfile_settings);
		
		if (ice_strncmp(word, "port", 4) == 0) {
			int p = atoi(line);
			for (i = 0; i < MAXLISTEN; i++)
				if (info.port[i] == 0) break;
			if (i < MAXLISTEN)
				info.port[i] = p;
			continue;
		}		
		
		if (!se) {
			android_log(ANDROID_LOG_VERBOSE, "Unknown setting %s on line %d", word, lineno);
			continue;
		}
		
		if (se->type == integer_e) {
			*(int *)(se->setting) = atoi(line);
		} else if (se->type == real_e) {
			*(double *)(se->setting) = atof(line);
		} else {
			if (*(char **)(se->setting) != NULL)
				nfree(*(char **)(se->setting));
			*((char **)(se->setting)) = clean_string(nstrdup(line));
		}
	}
	fd_close(cf);
	return 0;
}

void
write_401 (connection_t *con, char *realm)
{
	write_http_header (con->sock, 401, "Unauthorized");
	sock_write_line (con->sock, "WWW-Authenticate: Basic realm=\"%s\"", realm);
	sock_write_line (con->sock, "Content-Type: text/html");
	sock_write_line (con->sock, "Connection: close\r\n");

}

void
write_400 (connection_t *con)
{
	write_http_header (con->sock, 400, "Bad Request");
	sock_write_line (con->sock, "Content-Type: text/html");
	sock_write_line (con->sock, "Connection: close\r\n");

}

void
write_http_header(sock_t sockfd, int error, const char *msg)
{
	sock_write_line (sockfd, "HTTP/1.0 %i %s", error, msg);
	sock_write_line (sockfd, "Server: NTRIP NtripCaster %s/%s", info.version, info.ntrip_version);
}
