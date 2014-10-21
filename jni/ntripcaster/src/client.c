/* client.c
 * - Client functions
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
# ifndef __USE_BSD
#  define __USE_BSD
# endif
#include <string.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#include <sys/types.h>
#include <ctype.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>

#if defined (_WIN32)
#include <windows.h>
#define strncasecmp strnicmp
#else
#include <sys/socket.h> 
#include <sys/wait.h>
#include <netinet/in.h>
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "utility.h"
#include "ntrip_string.h"
#include "client.h"
#include "connection.h"
#include "log.h"
#include "source.h"
#include "sock.h"

/* basic.c. ajd ****************************************************/

#ifndef __EXTENSIONS__
#define __EXTENSIONS__
#endif
#include <time.h>

/* mount.c. ajd ****************************************************/

#ifdef HAVE_SIGNAL_H
#include <signal.h>
#endif


/* basic.c. ajd ****************************************************/

extern server_info_t info;
mutex_t authentication_mutex = {MUTEX_STATE_UNINIT};

mounttree_t *mounttree = NULL;
usertree_t *usertree = NULL;

time_t lastrehash = 0;

/* mount.c. ajd ****************************************************/


void client_login(connection_t *con, char *expr)
{
	char line[BUFSIZE];
	int go_on = 1;
	connection_t *source;
	request_t req;


	xa_debug(3, "Client login...\n");

	if (!con || !expr) {
		android_log(ANDROID_LOG_VERBOSE, "WARNING: client_login called with NULL pointer");
		return;
	}

	zero_request(&req);

	con->headervars = create_header_vars ();

	do {
		if (splitc(line, expr, '\n') == NULL) {
			strncpy(line, expr, BUFSIZE);
			go_on = 0;
		}

		if (ice_strncmp(line, "GET", 3) == 0) {
			build_request(line, &req);
		} else {
      			if (ice_strncmp(line, "Host:", 5) == 0 || (ice_strncmp(line, "HOST:", 5) == 0))
      				build_request(line, &req);
			else
				extract_header_vars (line, con->headervars);
		}
	} while (go_on);

	if (!authenticate_user_request (con, &req))
	{
		write_401 (con, req.path);
		kick_not_connected (con, "Not authorized");
		return;
	}

	if (((req.path[0] == '/') && (req.path[1] == '\0')) || (req.path[0] == '\0')) {
		send_sourcetable(con);
		kick_not_connected (con, "Sourcetable transferred");
		return;
	}
	
	if (strncasecmp(get_user_agent(con), "ntrip", 5) != 0) {
		write_401 (con, req.path);
		kick_not_connected (con, "No NTRIP client");
		return;	
	}

	xa_debug (1, "Looking for mount [%s:%d%s]", req.host, req.port, req.path);

	thread_mutex_lock (&info.double_mutex);
//	thread_mutex_lock (&info.mount_mutex);
	thread_mutex_lock (&info.source_mutex);

	source = find_mount_with_req (&req);

//	thread_mutex_unlock (&info.mount_mutex);
//	thread_mutex_unlock (&info.double_mutex);

	if (source == NULL)  {
	
		thread_mutex_unlock (&info.source_mutex);
		thread_mutex_unlock (&info.double_mutex);

		send_sourcetable(con);
		kick_not_connected (con, "Transfer Sourcetable");
		return;
	} else {
		if ((info.num_clients >= info.max_clients) 
		|| (source->food.source->num_clients >= info.max_clients_per_source))
		{
			thread_mutex_unlock (&info.source_mutex);
			thread_mutex_unlock (&info.double_mutex);

			if (info.num_clients >= info.max_clients)
				xa_debug (2, "DEBUG: inc > imc: %lu %lu", info.num_clients, info.max_clients);
			else if (source->food.source->num_clients >= info.max_clients_per_source)
				xa_debug (2, "DEBUG: snc > smc: %lu %lu", source->food.source->num_clients, info.max_clients_per_source);
			else 
				xa_debug (1, "ERROR: Erroneous number of clients, what the hell is going on?");
	
			kick_not_connected (con, "Server Full (too many listeners)");
			return;
		}

		put_client(con);
		con->food.client->type = listener_e;
		con->food.client->source = source->food.source;
		source->food.source->stats.client_connections++;
		if (req.user[0] != '\0') con->user = strdup(req.user);
		{
			const char *ref = get_con_variable (con, "Referer");
			if (ref && ice_strcmp (ref, "RELAY") == 0)
				con->food.client->type = pulling_client_e;
		}
		pool_add (con);
		greet_client(con, source->food.source);

	}

	thread_mutex_unlock (&info.source_mutex);
	thread_mutex_unlock (&info.double_mutex);

	util_increase_total_clients ();
/*
	// Change the sockaddr_in for the client to point to the port the client specified
	if (con->sin)
	{
		const char *sport = get_con_variable (con, "x-audiocast-udpport");
		if (sport)
		{
			con->sin->sin_port = htons (atoi (sport));
			con->food.client->use_udp = 1;
			xa_debug (1, "DEBUG: client_login(): Client listening on udp port %d", atoi (sport));
		}
	}
*/

	android_log(ANDROID_LOG_VERBOSE, "Accepted client %lu [%s] from [%s] on mountpoint [%s]. %lu clients connected", con->id,
		nullcheck_string(con->user), con_host (con), source->food.source->audiocast.mount, info.num_clients);

//	greet_client(con, source->food.source);
}

client_t *
create_client()
{
	client_t *client = (client_t *)nmalloc(sizeof(client_t));
	client->type = unknown_client_e;
	return client;
}

void put_client(connection_t *con)
{
	client_t *cli = create_client();
	con->food.client = cli;
	cli->errors = 0;
	cli->type = unknown_client_e;
	cli->write_bytes = 0;
	cli->virgin = -1;
	cli->source = NULL;
	cli->cid = -1;
	cli->offset = 0;
	cli->alive = CLIENT_ALIVE;
	con->type = client_e;
}

void util_increase_total_clients ()
{
	internal_lock_mutex (&info.misc_mutex);
	info.num_clients++;
	info.hourly_stats.client_connections++;
	internal_unlock_mutex (&info.misc_mutex);
}

void
util_decrease_total_clients ()
{
	internal_lock_mutex (&info.misc_mutex);
	info.num_clients--;
	internal_unlock_mutex (&info.misc_mutex);
}

void 
del_client(connection_t *client, source_t *source)
{
	if (!client || !source) {
		android_log(ANDROID_LOG_VERBOSE, "WARNING: del_client() called with NULL pointer");
		return;
	}

	if (source && client->food.client && (client->food.client->virgin != 1) && (client->food.client->virgin != -1)) {
		if (source->num_clients == 0)
			android_log (ANDROID_LOG_VERBOSE, "WARNING: Bloody going below limits on client count!");
		else
			source->num_clients--;
	}
	util_decrease_total_clients ();
}

void 
greet_client(connection_t *con, source_t *source)
{
#ifdef _WIN32
	int bufsize = 16384;
#endif
	
//	char *time;

	if (!con) {
		android_log(ANDROID_LOG_VERBOSE, "WARNING: greet_client called with NULL pointer");
		return;
	}

//	time = get_log_time();

	sock_write_line (con->sock, "ICY 200 OK");
//	sock_write_line (con->sock, "Server: NTRIP NtripCaster %s/%s", info.version, info.ntrip_version);
//	sock_write_line (con->sock, "Date: %s %s", time, info.timezone);
	
//	free (time);

	sock_set_blocking(con->sock, SOCK_NONBLOCK);

#ifdef _WIN32
	setsockopt(con->sock, SOL_SOCKET, SO_SNDBUF, (char *)&bufsize, sizeof(int));
#endif

	con->food.client->virgin = 1;

}

const char client_types[4][16] = { "listener", "pusher", "puller", "unknown listener" };

const char *
client_type (const connection_t *clicon)
{
	switch (clicon->food.client->type)
	{
		case listener_e:
			return client_types[0];
			break;
		case pulling_client_e:
			return client_types[2];
			break;
		default:
			return client_types[3];
			break;
	}
}

int
client_errors (const client_t *client)
{
	if (!client || !client->source)
		return 0;
	
	return (CHUNKLEN - (client->cid - client->source->cid)) % CHUNKLEN;
}

void
send_sourcetable (connection_t *con) {

	FILE *ifp;
	char szBuffer[2000], c[2];
	int nBytes = 1,
			nBufferBytes = 0,
			fsize = 0;
	char *time;

	time = get_log_time();

	sock_write_line (con->sock, "SOURCETABLE 200 OK");
	sock_write_line (con->sock, "Server: NTRIP NtripCaster %s/%s", info.version, info.ntrip_version);
//	sock_write_line (con->sock, "Date: %s %s", time, info.timezone);
	char pathBuffer[2000];
	sprintf(pathBuffer,"%s%s",info.runpath,"/sourcetable.dat");
	android_log(ANDROID_LOG_VERBOSE,"Sourcetable:%s",pathBuffer);
	ifp = fopen(pathBuffer,"r");
	if (ifp != NULL) {
		fseek(ifp, 0, SEEK_END);
		fsize = (int)ftell(ifp);
		rewind(ifp);

		sock_write_line (con->sock, "Content-Type: text/plain");
		sock_write_line (con->sock, "Content-Length: %d\r\n", fsize);

		while (nBytes > 0) {
			nBytes = fread(c,sizeof(char),sizeof(char),ifp);
			while (((unsigned int)c[0] != 10) && (nBytes > 0)) {
				szBuffer[nBufferBytes] = c[0];
				nBufferBytes++;
				nBytes = fread(c,sizeof(char),sizeof(char),ifp);
			}
			szBuffer[nBufferBytes] = '\0';
			if (nBufferBytes > 0) sock_write_line (con->sock, szBuffer);
			nBufferBytes = 0;
		}

		sock_write_line (con->sock, "ENDSOURCETABLE");
		fclose(ifp);
	} else sock_write_line (con->sock, "NO SOURCETABLE AVAILABLE");

	free (time);
}


/* basic.c. ajd ********************************************************************/

void rehash_authentication_scheme()
{
	int rehash_it = 0;
	struct stat st;
	char *mountfile;

	mountfile = get_icecast_file(info.configfile, conf_file_e, R_OK);

	if (!rehash_it && mountfile)
		if (stat(mountfile, &st) == 0) {
			if (st.st_mtime > lastrehash)
				rehash_it = 1;
		}
	if (rehash_it)
		parse_authentication_scheme();

	nfree(mountfile);
}

void init_authentication_scheme()
{
	thread_create_mutex(&authentication_mutex);
	
	mounttree = create_mount_tree();
  usertree = create_user_tree();

}

/*
 * Clean and setup authentication scheme.
 * Run every time any authentication file changes
 * Assert Class: 1
 */
void parse_authentication_scheme()
{
	thread_mutex_lock(&authentication_mutex);

	destroy_authentication_scheme();

	parse_mount_authentication_file();

	thread_mutex_unlock(&authentication_mutex);

	lastrehash = get_time();

}

void destroy_authentication_scheme()
{
	free_mount_tree(mounttree);
	free_user_tree(usertree);
}

int
authenticate_user_request(connection_t *con, request_t *req)
{

	ice_user_t checkuser, *authuser = NULL;
	mount_t *mount;

	//rehash_authentication_scheme();

	//print_authentication_scheme();

	if ((mount = need_authentication(req)) == NULL)
		return 1;
	else {	
		if (con_get_user(con, &checkuser) == NULL) return 0;

		xa_debug(1, "DEBUG: Checking authentication for mount %s for user %s with pass %s", nullcheck_string (req->path), nullcheck_string (checkuser.name),
			nullcheck_string (checkuser.pass));

		thread_mutex_lock(&authentication_mutex);

    authuser = find_user_from_tree(mount->usertree, checkuser.name);

		if (authuser != NULL) {

			if ((strncmp(checkuser.name, authuser->name, BUFSIZE) == 0) && (strncmp(checkuser.pass, authuser->pass, BUFSIZE) == 0)) {
				strncpy(req->user, checkuser.name, BUFSIZE);

				thread_mutex_unlock(&authentication_mutex);

				nfree(checkuser.name);
				nfree(checkuser.pass);
				return 1;
			}
		}

		thread_mutex_unlock(&authentication_mutex);

		xa_debug(1, "DEBUG: User authentication failed. Invalid user/password");
		nfree(checkuser.name);
		nfree(checkuser.pass);
	}

	return 0;

}

ice_user_t *
con_get_user(connection_t * con, ice_user_t * outuser)
{
	const char *cauth;
	char *decoded, *ptr;
	char user[BUFSIZE];
	char auth[BUFSIZE];
	char pass[BUFSIZE];

	if (!con || !outuser) {
		xa_debug(1, "WARNING: con_get_user() called with NULL pointers");
		return NULL;
	}
	outuser->name = NULL;
	outuser->pass = NULL;

	cauth = get_con_variable(con, "Authorization");

	if (!cauth)
		return NULL;

	strcpy(auth, cauth);

	splitc(NULL, auth, ' ');

	xa_debug(1, "DEBUG: con_get_user() decoding: [%s]", auth);

	ptr = decoded = util_base64_decode(auth);

	xa_debug(1, "DEBUG: con_get_user() decoded: [%s]", decoded);

	if (!splitc(user, decoded, ':')) {
		free(ptr);
		xa_debug(1, "DEBUG: con_get_user() Invalid authentication string");
		return NULL;
	}
	if (!splitc(pass, decoded, ':'))
		strcpy(pass, decoded);

	outuser->name = strdup(user);
	outuser->pass = strdup(pass);

	free(ptr);

	return outuser;
}

mount_t *need_authentication(request_t * req)
{
	mount_t *mount;
	mount_t search;

	xa_debug(3, "DEBUG: Checking need for authentication on mount %s", req->path);

	thread_mutex_lock(&authentication_mutex);

	search.name = req->path;

	mount = avl_find(mounttree, &search);

	if (mount) {
		thread_mutex_unlock(&authentication_mutex);
		return mount;
	}
	thread_mutex_unlock(&authentication_mutex);

	return NULL;
}

/* mount.c. ajd ***************************************************************************/

void parse_mount_authentication_file()
{
	int fd;
	char *mountfile = get_icecast_file(info.configfile, conf_file_e, R_OK);
	mount_t *mount;
	char line[BUFSIZE];

	if (!mountfile || ((fd = open_for_reading(mountfile)) == -1)) {
		if (mountfile)
			nfree(mountfile);
		xa_debug(1, "WARNING: Could not open config file for authentication scheme parsing");
		return;
	}
	while (fd_read_line(fd, line, BUFSIZE)) {
		if (line[0] != '/')
			continue;

		mount = create_mount_from_line(line);

		if (mount)
			add_authentication_mount(mount);
	}

	if (line[BUFSIZE-1] == '\0') {
		android_log(ANDROID_LOG_VERBOSE, "READ ERROR: too long authentication line in config file (exceeding BUFSIZE)");
	}

	if (mountfile)
		nfree(mountfile);
	fd_close(fd);
}

mount_t *
 create_mount_from_line(char *line)
{
	mount_t *mount;
	ice_user_t *user, *newuser;
	int go_on = 1;
	char cuser[BUFSIZE], name[BUFSIZE];

	if (!line) {
		xa_debug(1, "WARNING: create_mount_from_line() called with NULL pointer");
		return NULL;
	}
	if (!splitc(name, line, ':')) {
		xa_debug(1, "ERROR: Syntax error in mount file, with line [%s]", line);
		return NULL;
	}

	mount = create_mount();

	mount->name = nstrdup(clean_string(name));

	do {
		if (splitc(cuser, line, ',') == NULL) {
			strcpy(cuser, line);
			go_on = 0;
		}

		newuser = create_user_from_line(cuser);
		user = find_user_from_tree(usertree, newuser->name);

		if (user != NULL) {
			avl_insert(mount->usertree, user);
			nfree(newuser->name);
			nfree(newuser->pass);
			nfree(newuser);
		} else {
    	avl_insert(usertree, newuser);
			avl_insert(mount->usertree, newuser);
		}

		user = NULL;

	} while (go_on);

	return mount;
}

mount_t *
 create_mount()
{
	mount_t *mount = (mount_t *) nmalloc(sizeof (mount_t));

	mount->name = NULL;
	mount->usertree = create_user_tree();

	return mount;
}

mounttree_t *
 create_mount_tree()
{
	mounttree_t *gt = avl_create(compare_mounts, &info);

	return gt;
}

int add_authentication_mount(mount_t * mount)
{
	mount_t *out;

	if (!mount || !mounttree || !mount->name || !mount->usertree) {
		xa_debug(1, "ERROR: add_authentication_mount() called with NULL pointers");
		return 0;
	}
	out = avl_replace(mounttree, mount);

	if (out) {
		android_log(ANDROID_LOG_VERBOSE, "WARNING: Duplicate mount record %s, using latter", mount->name);
		nfree(out->name);
		avl_destroy(out->usertree, NULL);
		nfree(out);
	}
	xa_debug(1, "DEBUG: add_authentication_mount(): Inserted mount [%s]", mount->name);

	return 1;
}

void free_mount_tree(mounttree_t * mt)
{
	avl_traverser trav =
	{0};
	mount_t *mount, *out;

	if (!mt)
		return;

	while ((mount = avl_traverse(mt, &trav))) {
		out = avl_delete(mt, mount);

		if (!out) {
			xa_debug(1, "WARNING: Weirdness in mounttree!");
			continue;
		}
		nfree(mount->name);

		avl_destroy(mount->usertree, NULL);

		nfree(mount);
	}
}


/* added. ajd ******************************************************************************/


ice_user_t *
 create_user_from_line(char *line)
{
	ice_user_t *user;

	char name[BUFSIZE];

	if (!line) {
		xa_debug(1, "WARNING: create_user_from_line() called with NULL pointer");
		return NULL;
	}
	if (!splitc(name, line, ':')) {
		xa_debug(1, "ERROR: Syntax error in config file (authentication), with line [%s]", line);
		return NULL;
	}

	user = create_user();

	user->name = nstrdup(clean_string(name));
	user->pass = nstrdup(clean_string(line));

	return user;
}

ice_user_t *
 create_user()
{
	ice_user_t *user = (ice_user_t *) nmalloc(sizeof (ice_user_t));

	user->name = NULL;
	user->pass = NULL;
	return user;
}

usertree_t *
 create_user_tree()
{
	usertree_t *ut = avl_create(compare_users, &info);

	return ut;
}

void free_user_tree(usertree_t * ut)
{
	avl_traverser trav = {0};
	ice_user_t *user, *out;

	if (!ut)
		return;

	while ((user = avl_traverse(ut, &trav))) {
		out = avl_delete(ut, user);

		if (!out) {
			xa_debug(1, "WARNING: Weirdness in usertree!");
			continue;
		}

		nfree(user->name);
		nfree(user->pass);
		nfree(user);
	}
}

/* must have authentication_mutex. ajd */
ice_user_t *
find_user_from_tree(usertree_t * ut, const char *name)
{
	ice_user_t search;

	search.name = strchr(name, name[0]);

	if (!ut || !name) {
		xa_debug(1, "WARNING: find_user_from_tree() called with NULL pointers");
		return NULL;
	}

	return avl_find(ut, &search);
}

void
print_authentication_scheme() {

	avl_traverser trav = {0}, trav2 = {0};
	ice_user_t *user;
	mount_t *mount;
	int i=0;

	thread_mutex_lock(&authentication_mutex);

	printf("\nMounttree:\n");
	while ((mount = avl_traverse(mounttree, &trav))) printf("%d-%s\n",++i,mount->name);
	i=0;
	zero_trav(&trav);

	printf("\nGlobal Usertree:\n");
	while ((user = avl_traverse(usertree, &trav))) printf("%d-%s:%s\n",++i,user->name, user->pass);
	i=0;
	zero_trav(&trav);

	printf("\nMounttree with Usertrees:\n");
	while ((mount = avl_traverse(mounttree, &trav))) {
		printf("%d-%s:\n",++i,mount->name);		
		while ((user = avl_traverse(mount->usertree, &trav2))) {
			printf("   %s:%s\n", user->name, user->pass);	
		}
		zero_trav(&trav2);
	}

	thread_mutex_unlock(&authentication_mutex);

}

