/* source.h
 * - Source Function Headers
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

#ifndef __ICECAST_SOURCE_H
#define __ICECAST_SOURCE_H

source_t *create_source();
void source_login(connection_t *con, char *line);
void kick_source(source_t *sor, char *why);
void *source_func(void *con);
void put_source(connection_t *con);
void add_source ();
void del_source ();
connection_t *find_mount_with_req (request_t *req);
void add_chunk (connection_t *sourcecon);
void write_chunk (source_t *source, connection_t *clicon);
void kick_clients_on_cid (source_t *source);
void kick_dead_clients (source_t *source);
int write_data (connection_t *clicon, source_t *source);
int finish_meta_frame (connection_t *clicon);
const char *sourcetype_to_string (source_type_t type);
int start_chunk (source_t *source);
void source_write_to_client (source_t *source, connection_t *clicon);
void source_get_new_clients (source_t *source);
#endif
