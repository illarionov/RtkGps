/* client.h
 * - Client function headers
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

#ifndef __ICECAST_CLIENT_H
#define __ICECAST_CLIENT_H


void client_login(connection_t *con, char *line);
void put_client(connection_t *con);
client_t *create_client();
void util_increase_total_clients ();
void util_decrease_total_clients ();
void del_client(connection_t *client, source_t *source);
int client_errors (const client_t *client);
void greet_client(connection_t *con, source_t *source);
const char *client_type (const connection_t *clicon);
void send_sourcetable (connection_t *con);
#endif


/* basic.h. ajd ********************************************************************/

typedef avl_tree mounttree_t;
typedef avl_tree usertree_t;

typedef struct mountSt {
	char *name;
	usertree_t *usertree;
} mount_t;

typedef struct userSt {
	char *name;
	char *pass;
} ice_user_t;

void init_authentication_scheme();
void parse_authentication_scheme();
void destroy_authentication_scheme();
int authenticate_user_request(connection_t * con, request_t * req);
mount_t *need_authentication(request_t * req);
void rehash_authentication_scheme();
ice_user_t *con_get_user(connection_t * con, ice_user_t * outuser);

/* mount.h.ajd ****************************************************/

void parse_mount_authentication_file();
mount_t *create_mount_from_line(char *line);
mount_t *create_mount();
mounttree_t *create_mount_tree();
int add_authentication_mount(mount_t * mount);
void free_mount_tree(mounttree_t * mt);
int runtime_add_mount(const char *name);
int runtime_add_mount_with_group(const char *name, char *groups);

/* added. ajd ******************************/
ice_user_t *create_user_from_line(char *line);
ice_user_t *create_user();
usertree_t *create_user_tree();
void free_user_tree(usertree_t *ut);
ice_user_t *find_user_from_tree(usertree_t * ut, const char *name);

void print_authentication_scheme();

