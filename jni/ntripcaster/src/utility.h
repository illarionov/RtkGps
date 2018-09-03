/* utility.h
 * - Utility Function Headers
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

#ifndef __ICECAST_UTILITY_H
#define __ICECAST_UTILITY_H

typedef struct
{
	char *name;                   /* Printable name of this setting */
	type_t type;                  /* Integer, string or double */
	char *doc;
	void *setting;                /* The actual data affected by this setting */
} set_element;

char *clean_string_from_spaces(char *string);
char *clean_string_from_leading_spaces(char *string);
void clean_away_source(source_t *source);
void assign_old_listeners(source_t *ser);
int map_id_to_source_socket(char *idstring);
source_t *source_with_id(int id);
int password_match(const char *crypted, const char *uncrypted);
int check_pass(int sockfd, char *pass, int *counter, char *string);
int find_frame_ofs(source_t *source);
void kick_connection_not_me (void *conarg, void *reasonarg);
void kick_connection(void *conarg, void *reasonarg);
void kick_everything();
void kick_not_connected (connection_t *con, char *reason);
void close_connection(void *data, void *param);
void close_socket(sock_t sock);
void threaded_detach ();
int server_detach();
connection_t *find_source_with_mount (char *mount);
void kill_threads ();
unsigned long int new_id ();
time_t tree_time(avl_tree *tree);
void write_icecast_header ();
void print_startup_server_info ();
unsigned long int transfer_average (unsigned long int bytes, unsigned long int connections);
char *connect_average (unsigned long int seconds, unsigned long int connections, char *buf);
void clear_source_stats (void *data, void *param);
void clear_client_stats (void *data, void *param);
int hostname_local (char *name);
void build_request (char *line, request_t *req);
int mount_exists (char *mount);
void zero_request (request_t *req);
void generate_request (char *line, request_t *req);
void generate_http_request (char *line, request_t *req);
void init_thread_tree (int line, char *file);
char *next_mount_point();
void pending_connection (connection_t *con);
int pending_source_signoff (connection_t *con);
int open_for_reading (const char *filename);
int open_for_append (const char *filename);
char *get_icecast_file (const char *filename, filetype_t type, int flags);
char *get_template (const char *filename);
char *get_log_file (const char *filename);
void stat_add_write (statistics_t *stat, int len);
void stat_add_read (statistics_t *stat, int len);
char * type_of_str (contype_t type, char *buf);
void my_sleep (int microseconds);
void show_runtime_configuration ();
int is_recoverable (int error);
void set_run_path (char *path);
void dispose_audiocast (audiocast_t *au);
int is_valid_http_request (request_t *req);
void free_con (connection_t *con);
int count_clients();
void sleep_random(int max);
void setup_config_file_settings();
set_element *find_set_element (char *name, set_element *el);
int parse_default_config_file();
int parse_config_file(char *file);
void write_401 (connection_t *con, char *realm);
void write_400 (connection_t *con);
void write_http_header(sock_t sockfd, int error, const char *msg);
source_t *source_with_client(connection_t *con);



#ifndef _WIN32
#define min(x,y) ((x)<(y)?(x):(y))
#endif

#endif


