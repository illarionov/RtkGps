/* main.c
 * - Main Program
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
#include <sys/stat.h>
#include <netinet/in.h>
#include <netdb.h>
# ifdef TIME_WITH_SYS_TIME
#  include <sys/time.h>
# endif
#else
#include <winsock.h>
#endif

#ifdef HAVE_SYS_RESOURCE_H
#include <sys/resource.h>
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "sock.h"
#include "log.h"
#include "main.h"
#include "utility.h"
#include "ntrip_string.h"
#include "source.h"
#include "client.h"
#include "connection.h"
#include "timer.h"

#ifndef _WIN32
#include <signal.h>
#endif

/* globals.h. ajd *******************************************/
#ifndef _ICECAST_GLOBALS_H
#define _ICECAST_GLOBALS_H
int running = 0;
#endif


/* for perror and for various sanity checks */
/* extern int errno; */

/* Importing a tree and mutex from sock.c */
extern avl_tree *sock_sockets;
extern mutex_t sock_mutex;

/* global */
server_info_t info;
struct in_addr localaddr;


#ifndef _WIN32
void 
increase_maximum_number_of_open_files()
{
#if defined(HAVE_SETRLIMIT) && defined (HAVE_GETRLIMIT)
	struct rlimit before, after;
	
	if (getrlimit (RLIMIT_NOFILE, &before) == 0) {
		xa_debug (1, "DEBUG: Max number of open files: soft: %d hard: %d",
			  (int)before.rlim_cur, (int)before.rlim_max);
	} else {
		xa_debug (1, "WARNING: getrlimit() failed.");
		return;
	}

	after.rlim_cur = info.max_clients + info.max_sources + 20;
	after.rlim_max = before.rlim_max > after.rlim_cur ? before.rlim_max : after.rlim_cur;
	
	if (setrlimit (RLIMIT_NOFILE, &after) == 0) 
	{
		xa_debug (1, "DEBUG: Max number of open files raised from: soft %d hard: %d, to soft: %d hard: %d", before.rlim_cur, before.rlim_max, after.rlim_cur, after.rlim_max);
	} else {
		android_log (ANDROID_LOG_VERBOSE, "ERROR: Increasing maximum number of open files from %lu:%lu to: %lu:%lu failed, try lowering the maximum values for listeners, admins, and sources.", before.rlim_cur, before.rlim_max, after.rlim_cur, after.rlim_max);
		android_log (ANDROID_LOG_VERBOSE, "WARNING: The server will run out of file descriptors before the reaching specified limits!");
	}
#endif
}
#endif

void 
initialize_network()
{
#ifdef _WIN32
	WSADATA wsad;
	
	/* Initialize Winsock*/
	WSAStartup(0x0101, &wsad);
#else
	/* On some systems increase the max number of open files. */
	increase_maximum_number_of_open_files();

	/* Create a tcp socket for DNS queries that stays open */
	/* sethostent(1); */
#endif
}

/* Print header, select the console mode, and start the main server loop. */
void
startup_mode()
{
	/* Try to open the log files */
	open_log_files();

	/* Write startup information in the log file, and print header on stdout */
	write_icecast_header();
  
	/* Set the running flag */
	running = SERVER_RUNNING;

#ifdef _WIN32
		android_log(ANDROID_LOG_VERBOSE, "Using stdout as NtripCaster logging window");
#else
	if (info.console_mode == 1)
	{
		//server_detach();
		info.detach = 1;
	} else {
		android_log(ANDROID_LOG_VERBOSE, "Using Android Log as NtripCaster logging window");
	}
#endif

	threaded_server_proc(&info); /* Never returns */
}

void
setup_signal_traps()
{
	xa_debug (1, "DEBUG: Activating signal handler");

#ifdef _WIN32
	if (!SetConsoleCtrlHandler( win_sig_die, 1 ))
		android_log(ANDROID_LOG_VERBOSE, "FAILED setting up win32 signal handler");
#else
	
# if (defined(SYSV) && !defined(hpux)) || defined(SVR4)
#  define signal sigset
# endif
	
	signal(SIGHUP, sig_hup);
	signal(SIGINT, sig_die);
	signal(SIGTERM, sig_die);
	signal(SIGCHLD, sig_child);
	signal(SIGPIPE, SIG_IGN);
#endif
}

/* Set all global variables to their default values */
void 
setup_defaults()
{
	int i;

	xa_debug (1, "DEBUG: Setting up default values");

	info.consoledebuglevel = 0;
	info.logfiledebuglevel = 0;
	info.console_mode = DEFAULT_CONSOLE_MODE;

#ifdef HAVE_UMASK
	{
	  mode_t before, after = 022;
	  before = umask(after);
	  xa_debug(1, "DEBUG: Changed umask from %d to %d", before, after);
	}
#endif

	/* Create the data locking mutexes */
	thread_create_mutex(&info.double_mutex);
	thread_create_mutex(&info.source_mutex);
	thread_create_mutex(&info.misc_mutex);
	thread_create_mutex(&info.mount_mutex);
	thread_create_mutex(&info.hostname_mutex);
	thread_create_mutex(&info.resolvmutex);

#ifdef DEBUG_SOCKETS
	thread_create_mutex(&sock_mutex);
#endif
	info.resolv_type = DEFAULT_RESOLV_TYPE;

	memset((void *)&localaddr, 0, sizeof (localaddr));

	/* Setup main thread */
	info.main_thread = thread_self();

	info.detach = 0;

	/* Default value for hostname reverse lookups */
	info.reverse_lookups = DEFAULT_LOOKUPS;

	/* Statistics */
	zero_stats(&info.daily_stats);
	zero_stats(&info.hourly_stats);
	zero_stats(&info.total_stats);

	info.server_start_time = get_time();
	info.sleep_ratio = (double)DEFAULT_SLEEP_RATIO;
	info.bandwidth_usage = 0;

	info.id = 0;

	info.port[0] = DEFAULT_PORT;
	for (i = 1; i < MAXLISTEN; i++) {
		info.port[i] = 0;
	}

	/* Variables that affect clients */
	info.num_clients = 0;
	info.max_clients = DEFAULT_MAX_CLIENTS;
	info.max_clients_per_source = DEFAULT_MAX_CLIENTS_PER_SOURCE;
	info.client_timeout = DEFAULT_CLIENT_TIMEOUT;
	info.client_pass = nstrdup(DEFAULT_CLIENT_PASSWORD);

	/* Variables that affect sources */
	info.num_sources = 0;
	info.max_sources = DEFAULT_MAX_SOURCES;
	info.encoder_pass = nstrdup(DEFAULT_ENCODER_PASSWORD);
	info.statustime = DEFAULT_STATUSTIME;
	info.myhostname = NULL;
	info.server_name = nstrdup("localhost");

	info.version = VERSION;
	info.ntrip_version = DEFAULT_NTRIP_VERSION;
	info.timezone = get_string_time(get_time(), "%Z");
	if (!info.timezone) info.timezone = "";

	if (!info.runpath) 
		android_log (ANDROID_LOG_ERROR, "WARNING: info.runpath == NULL!!\n");

	info.logdir = nstrdup(DEFAULT_LOG_DIR);
	if (info.logdir[0] != DIR_DELIMITER) {
		nfree(info.logdir);
		info.logdir = nmalloc(strlen(info.runpath) + strlen(DEFAULT_LOG_DIR) + 1);
		strcpy(info.logdir, info.runpath);
		strcat(info.logdir, DEFAULT_LOG_DIR);
	}
	info.etcdir = nstrdup(DEFAULT_ETC_DIR);
	if (info.etcdir[0] != DIR_DELIMITER) {
		nfree(info.etcdir);
		info.etcdir = nmalloc(strlen(info.runpath) + strlen(DEFAULT_ETC_DIR) + 1);
		strcpy(info.etcdir, info.runpath);
		strcat(info.etcdir, DEFAULT_ETC_DIR);
	}

	info.configfile = nstrdup(DEFAULT_CONFIG_FILE);
	info.logfilename = nstrdup(DEFAULT_LOGFILE);
	info.logfile = -1;

	/* Server meta info */
	info.location = nstrdup(DEFAULT_LOCATION);
	info.rp_email = nstrdup(DEFAULT_RP_EMAIL);
	info.server_url = nstrdup(DEFAULT_SERVER_URL);

	setup_config_file_settings();
}

void
allocate_resources()
{  
	xa_debug (1, "DEBUG: Allocating server resources");

	/* Allocate all the sources. */
	info.sources = avl_create(compare_connection, &info);

	info.my_hostnames = avl_create(compare_strings, &info);

#ifdef DEBUG_SOCKETS
	sock_sockets = avl_create (compare_sockets, &info);
#endif
	
	pool_init ();

	if (!info.sources || !info.threads || !info.my_hostnames) {
		fprintf(stderr, "Cannot allocate tree resources, exiting");
		clean_shutdown(&info);
	}

}

void mutex_locking_timeout(void *param)
{
	android_log(ANDROID_LOG_VERBOSE, "mutex_locking_timeout...");
	mutex_t *mutex;
	mutex = (mutex_t *)param;
	internal_lock_mutex(mutex);
	pthread_exit(0);
}

/* Shutdown server, make sure sockets are closed, free up the memory */
void 
clean_shutdown (server_info_t *info)
{
	connection_t *con;
	int i;
	avl_traverser trav = {0};
	pthread_t mutex_locking_thread;
	static int main_shutting_down = 0;
	
	android_log(ANDROID_LOG_VERBOSE, "Starting shutting down...");
	thread_library_lock ();
	android_log(ANDROID_LOG_VERBOSE, "Thread library locked...");
		if (!main_shutting_down)
		{
			main_shutting_down = 1;
		}
		else
		{
			android_log(ANDROID_LOG_VERBOSE, "thread_exit (0)...");
			thread_exit (0);
		}
	android_log(ANDROID_LOG_VERBOSE, "Thread library unlocking...");
	thread_library_unlock ();
	
	android_log(ANDROID_LOG_VERBOSE, "Cleanly shutting down...");
	android_log(ANDROID_LOG_VERBOSE, "Closing all listening sockets...");

	for (i = 0; i < MAXLISTEN; i++) 
	{
		if (sock_valid (info->listen_sock[i]))
			sock_close(info->listen_sock[i]);
	}
	
	pool_shutdown ();

	kill_threads();
	
	/* Close all remaining sockets */
	sock_close_all_sockets ();

	/* Wait for the last threads to die  */
	thread_wait_for_solitude ();

	android_log(ANDROID_LOG_VERBOSE, "Mutex locking with timeout...");
	pthread_create(&mutex_locking_thread, NULL, (void*)mutex_locking_timeout,(void*)&info->source_mutex);
	if (pthread_join_timeout(mutex_locking_thread, 300))
	{
		android_log(ANDROID_LOG_VERBOSE, "Joining thread timed out...");
	}

	android_log(ANDROID_LOG_VERBOSE, "Removing remaining sources...");
	while ((con = avl_traverse(info->sources, &trav)))
		kick_connection(con, "Server shutting down");

	thread_mutex_unlock(&info->source_mutex);

	zero_trav(&trav);

#ifdef _WIN32
	/* Cleanup Winsock */
	WSACleanup();
#endif

	android_log(ANDROID_LOG_VERBOSE, "Exiting..");
//	if (info->logfile != -1)
//		fd_close(info->logfile);
	
#ifdef DEBUG_MEMORY
	{
		meminfo_t *mi;
		avl_traverser trav = {0};
		
		while ((mi = avl_traverse(info->mem, &trav))) {
			if (mi->thread_id != 0 && mi->thread_id != -1)
				android_log(ANDROID_LOG_VERBOSE, "WARNING: %d bytes allocated by thread %d at line %d in %s not freed before thread exit", mi->size, mi->thread_id, mi->line, mi->file);
		}
	}
#endif
	pthread_exit(0);
	//thread_exit(0);
}

/* Main server loop, listen to the specified socket for new
 * connections, and immediately start a new thread for each
 * new connection */
void *
threaded_server_proc (void *infoarg)
{
	connection_t *con;
	mythread_t *mt = thread_get_mythread ();

	android_log(ANDROID_LOG_VERBOSE, "Starting main connection handler...");
  
	/* Setup listeners */
	setup_listeners();

	/* Just print some runtime server info */
	print_startup_server_info();

	android_log (ANDROID_LOG_VERBOSE, "Starting Calender Thread...");
	/* Fork another thread that handles time based actions */
	thread_create("Calendar Thread", startup_timer_thread, NULL);
	
	while (running == SERVER_RUNNING)
	{
		/* Try to get a new connection */
		con = get_connection(info.listen_sock);
		
		if (con) {
			/* handle the new connection it in a new thread */
			thread_create("Connection Handler", handle_connection, (void *)con);
		}
		
		if (mt->ping == 1)
			mt->ping = 0;
	}
  
	/* user pressed ^C */
	clean_shutdown(&info);

	return NULL;
}

#ifdef _WIN32

BOOL WINAPI 
win_sig_die(DWORD CtrlType)
{
	android_log(ANDROID_LOG_VERBOSE, "Caught signal %d, perhaps someone is at the door?", CtrlType);
	running = SERVER_DYING;
	return 1;
}

#else

RETSIGTYPE 
sig_child(int signo)
{
	pid_t pid;
	int stat;
  
	pid = wait(&stat);
#ifdef __linux__
	signal(SIGCHLD, sig_child);
#endif
}

RETSIGTYPE 
sig_hup(int signo)
{
	parse_default_config_file();
	open_log_files();
	
	android_log(ANDROID_LOG_VERBOSE, "Caught SIGHUP, rehashed config and reopened logfiles...");
	
	signal(SIGHUP, sig_hup);
}

RETSIGTYPE 
sig_die(int signo)
{
	android_log(ANDROID_LOG_VERBOSE, "Caught signal %d, perhaps someone is at the door?", signo);
	running = SERVER_DYING;
}

int get_ntripcaster_state()
{
	return running;
}

RETSIGTYPE 
sig_die_hard(int signo)
{
	printf("Caught signal %d, shutting down!\n", signo);
	exit(1);
}

#endif

/* 
 * Create and listen to the specified ports, 
 * find out local ip if dynamic,
 * make sure the server name is resolvable 
 */
void 
setup_listeners()
{
	int i;

	for (i = 0; i < MAXLISTEN; i++)
		info.listen_sock[i] = INVALID_SOCKET;

	/* Create the socket, on the correct hostname or INADDR_ANY and bind it to the port. */
	for (i = 0; i < MAXLISTEN; i++) 
	{
		if (info.port[i] <= 0) {
			info.port[i] = INVALID_SOCKET;
			continue;
		}

		info.listen_sock[i] = sock_get_server_socket(info.port[i]);
  
		if (info.listen_sock[i] == INVALID_SOCKET) 
		{
			android_log(ANDROID_LOG_VERBOSE, "ERROR: Could not listen to port %d. Perhaps another process is using it?", info.port[i]);
			clean_shutdown(&info);
		}

		/* Set the socket to nonblocking */
		sock_set_blocking(info.listen_sock[i], SOCK_NONBLOCK);

		if (listen(info.listen_sock[i], LISTEN_QUEUE) == SOCKET_ERROR) 
		{
			android_log(ANDROID_LOG_VERBOSE, "Could not listen for clients on port %d", info.port[i]);
			clean_shutdown(&info);
		} 
	}
	
	if (ice_strcasecmp(info.server_name, "dynamic") == 0) 
	{
		info.server_name = sock_get_local_ipaddress();
		android_log(ANDROID_LOG_VERBOSE, "Dynamic server name, using the local ip [%s]", info.server_name);
	} else {
		char *res, *buf = (char *)nmalloc(20);
		res = forward(info.server_name, buf);
		if (!res) {
			nfree(buf);
			android_log(ANDROID_LOG_VERBOSE, "WARNING: Resolving the server name [%s] does not work!", info.server_name);
			return;
		}
		
		thread_mutex_lock(&info.hostname_mutex);
		
		avl_insert(info.my_hostnames, info.server_name);
		avl_insert(info.my_hostnames, res);
		
		thread_mutex_unlock(&info.hostname_mutex);
	}
}



