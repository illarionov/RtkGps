/* ntripcaster.h
 * - Configuration Information
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

#ifndef __ICECAST_CONFIG_H
#define __ICECAST_CONFIG_H

#ifdef _WIN32
#include "win32config.h"
#else
#include "config.h"
#endif

#define SERVER_RUNNING 1
#define SERVER_INITIALIZING 2
#define SERVER_DYING 0
#define OK 1
#define UNKNOWN -1
#define ICE_ERROR_CONNECT -1
#define ICE_ERROR_INVALID_SYNTAX -2
#define ICE_ERROR_ARGUMENT_REQUIRED -3
#define ICE_ERROR_HEADER -4
#define ICE_ERROR_TRANSMISSION -5
#define ICE_ERROR_DUPLICATE -6
#define ICE_ERROR_FILE -7
#define ICE_ERROR_INVALID_PASSWORD -8
#define ICE_ERROR_NO_SUCH_MOUNT -9
#define ICE_ERROR_NULL -10
#define ICE_ERROR_NOT_FOUND -11
#define ICE_ERROR_INIT_FAILED -12
#define ICE_ERROR_INSERT_FAILED -13
#define ICE_ERROR_NOT_INITIALIZED -14
#define ICE_ERROR_MOUNTPOINT_TAKEN -15
#define DEFAULT_SLEEP_RATIO 0.10
#define DEFAULT_MOUNT_FALLBACK 1
#define DEFAULT_FORCE_SERVERNAME 0
#define DEFAULT_ICE_ROOT "."
#define DEFAULT_CLIENT_TIMEOUT 0
#define DEFAULT_LOOKUPS 0
#define DEFAULT_PORT 8000

#ifdef SOMAXCONN
#define LISTEN_QUEUE SOMAXCONN
#else
#define LISTEN_QUEUE 50
#endif

#define DEFAULT_MAX_CLIENTS 100
#define DEFAULT_MAX_CLIENTS_PER_SOURCE 100
#define DEFAULT_MAX_SOURCES 40
#define DEFAULT_ENCODER_PASSWORD "sesam01"
#define DEFAULT_CLIENT_PASSWORD "prettyplease"
#define DEFAULT_LOGFILE "ntripcaster.log"
#define DEFAULT_LOG_DIR "."
#define DEFAULT_ETC_DIR "."
#define DEFAULT_CONFIG_FILE "ntripcaster.conf"
#define DEFAULT_STATUSTIME 60
#define DEFAULT_LOCATION "BKG Geodetic Department"
#define DEFAULT_RP_EMAIL "euref-ip@bkg.bund.de"
#define DEFAULT_SERVER_URL "http://igs.ifag.de/"
#define DEFAULT_KICK_CLIENTS 1
#define DEFAULT_CONSOLE_MODE 0
#define DEFAULT_NTRIP_VERSION "1.0"

#if defined (SOLARIS) && defined (HAVE_GETHOSTBYNAME_R) && defined (HAVE_GETHOSTBYADDR_R)
# define DEFAULT_RESOLV_TYPE solaris_gethostbyname_r_e
# define SOLARIS_RESOLV_OK 1
#elif defined (LINUX) && defined (HAVE_GETHOSTBYNAME_R) && defined (HAVE_GETHOSTBYADDR_R)
# define DEFAULT_RESOLV_TYPE linux_gethostbyname_r_e
# define LINUX_RESOLV_OK 1
#else
# define DEFAULT_RESOLV_TYPE standard_gethostbyname_e
# define DEFAULT_RESOLV_OK 1
#endif

#define SOCK_UNUSED -1
#define SOCK_SIGNOFF -2
#define SOURCE_KILLED 0
#define SOURCE_CONNECTED 1
#define SOURCE_UNUSED 2
#define SOURCE_PAUSED 3
#define SOURCE_PENDING 4
#define CLIENT_ALIVE 1
#define CLIENT_DEAD 0
#define CLIENT_PAUSED 3
#define CLIENT_UNPAUSED 4
#define CLIENT_MOVE 5
#undef DEBUG_MEMORY
#undef DEBUG_MEMORY_MCHECK 
#undef DEBUG_MUTEXES
#undef DEBUG_SOCKETS
#undef DEBUG_SLEEP
#undef DEBUG_FULL 
#undef OPTIMIZE
#undef SAVE_CPU
#define ANDROID_LOG_VERBOSE 0

#endif

/* icetypes.h. ajd ****************************************************************************/
#ifndef __ICECAST_TYPES_H
#define __ICECAST_TYPES_H

typedef enum {listener_e = 0, pulling_client_e = 2, unknown_client_e = -1 } client_type_t;
typedef enum {icy_e = 0 } protocol_t;
typedef enum {encoder_e = 0, puller_e = 1, on_demand_pull_e = 2, unknown_source_e = -1 } source_type_t;
typedef enum contype_e {client_e = 0, source_e = 1, unknown_connection_e = 3 } contype_t;
typedef enum { conf_file_e = 1, log_file_e = 2 } filetype_t;
typedef enum { linux_gethostbyname_r_e = 1, solaris_gethostbyname_r_e = 2, standard_gethostbyname_e = 3 } resolv_type_t;
typedef int icecast_function();
typedef avl_tree vartree_t;
typedef int wid_t;
typedef enum type_e {integer_e, real_e, string_e, function_e} type_t;

#define BUFSIZE 1000
#define CHUNKLEN 64
#define MAXMETADATALENGTH (100)
#define SOURCE_BUFFSIZE 1000
#define SOURCE_READSIZE (100)
#define MAXLISTEN 5		/* max number of listening ports */

#ifndef HAVE_SOCKLEN_T
//typedef int socklen_t;
#endif

#ifndef _WIN32
typedef int SOCKET;
#define DIR_DELIMITER '/'
#else
#define DIR_DELIMITER '\\'
#define W_OK 2
#define R_OK 3
#endif

#ifndef HAVE_SOCK_T
typedef int sock_t;
#else
typedef sock_t SOCKET;
#endif

typedef struct varpair_St
{
	char *name;
	char *value;
} varpair_t;

typedef struct request_St
{
	char path[BUFSIZE];
	char host[BUFSIZE];
	char user[BUFSIZE];
	int port;
} request_t;

typedef struct chunkSt
{
	char data[SOURCE_BUFFSIZE + MAXMETADATALENGTH];
	int len;
	int metalen;
	int clients_left;
} chunk_t;

typedef struct statistics_St
{
	unsigned long int read_bytes;   /* Bytes read from encoder(s) */
  unsigned long int read_kilos;   /* Kilos read from encoder(s) */
	unsigned long int write_bytes;  /* Bytes written to client(s) */
  unsigned long int write_kilos;  /* Kilos written to client(s) */
	unsigned long int client_connections; /* Number of connects from clients */
	unsigned long int source_connections; /* Number of connects from sources */
	unsigned long int client_connect_time; /* Total sum of the time each client has been connected (minutes) */
	unsigned long int source_connect_time; /* Total sum of the time each source has been connected (minutes) */
} statistics_t;

typedef struct audiocast_St {
	char *name; //		 Name of Server
	char *mount;	//	 Name of source
} audiocast_t;

typedef struct source_St {
	int connected;
	source_type_t type;
	protocol_t protocol;
	mutex_t mutex;
	audiocast_t audiocast;
	avl_tree *clients;             /* Tree of clients */
	icethread_t thread;              /* Pointer to running thread */
	statistics_t stats;
	unsigned long int num_clients;
	chunk_t chunk[CHUNKLEN];
	int cid;
	int priority;
	char *source_agent;

} source_t;

typedef struct client_St {
	unsigned int use_udp:1;
	unsigned int use_icy:1;
 	int errors;             /* Used at first to mark position in buf, later to mark error */
	int offset;
	int cid;
	int alive;
	client_type_t type;
	unsigned long int write_bytes;	/* Number of bytes written to client */
	int virgin;
	source_t *source;        /* Pointer back to the source */
} client_t;

typedef struct connectionSt {
	contype_t type;
	union {
		client_t *client;
		source_t *source;
	} food;
	unsigned long int id;  /* Session unique connection id */
	struct sockaddr_in *sin;
	socklen_t sinlen;
	SOCKET sock;
	time_t connect_time;
	char *host;
	char *hostname;
	vartree_t *headervars;
	char *user;
} connection_t;

typedef struct {
	char *runpath;			/* argv[0] */
	int port[MAXLISTEN];
	SOCKET listen_sock[MAXLISTEN];	/* Socket to listen to */
	char *etcdir;		/* Name of config file directory */
	char *logdir;
	avl_tree *sources;
	unsigned long int num_sources;
	unsigned long int max_sources;
	char *encoder_pass;	/* Password to verify encoder */
	char *configfile;	/* Name of configuration file */
	char *mountfile;
	char *logfilename;	/* Name of default log file */
  int logfile;		/* File descriptor for the logfile */
	time_t statslasttime;	
	long server_start_time; /* The time the server started */
	time_t statuslasttime;
	int statustime;
	char *myhostname;	/* NULL unless bind to specific ip */
	char *server_name;	/* Server name */
	char *version;
	char *ntrip_version;
	char *timezone;
	int detach;
	double bandwidth_usage;
	double sleep_ratio;
	int reverse_lookups;
	int force_servername;
	int mount_fallback;
	icethread_t main_thread;
#ifndef _WIN32
	pthread_attr_t defaultattr;
#endif
	mutex_t source_mutex;
	mutex_t misc_mutex;
	mutex_t mount_mutex;
	mutex_t hostname_mutex;
	mutex_t double_mutex;
	mutex_t thread_mutex;
	mutex_t mutex_mutex;
#ifdef DEBUG_MEMORY
	mutex_t memory_mutex;
	avl_tree *mem;
#endif
	mutex_t resolvmutex;
	resolv_type_t resolv_type;
	int client_timeout;
	char *client_pass;
	unsigned long int num_clients;
	unsigned long int max_clients;
	unsigned long int max_clients_per_source;
	avl_tree *threads;
	avl_tree *mutexes;
	long int threadid;
	long int mutexid;
	unsigned long int id;
	int kick_clients;
	avl_tree *my_hostnames;
	statistics_t hourly_stats;
	statistics_t daily_stats;
	statistics_t total_stats;
	char *location;
	char *rp_email;
	char *server_url;
	int consoledebuglevel;
	int logfiledebuglevel;

	int console_mode;

} server_info_t;

#endif

