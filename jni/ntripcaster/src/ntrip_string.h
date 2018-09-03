/* ntrip_string.h
 * - Headers for the string functions
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

#ifndef __ICECAST_ICE_STRING_H
#define __ICECAST_ICE_STRING_H

char *splitc(char *first, char *rest, const char divider);
char *clean_string(char *string);
const char *con_host (connection_t *con);
char *my_strdup (const char *string);
char *util_base64_decode(char *message);
char *mutex_to_string (mutex_t *mutex, char *out);
char *create_malloced_ascii_host(struct in_addr *in);
char *makeasciihost(const struct in_addr *in, char *buf);
char *nice_time(unsigned long int seconds, char *buf);
int ice_strcasecmp (const char *s1, const char *s2);
int ice_strncmp (const char *s1, const char *s2, size_t n);
int ice_strcmp (const char *s1, const char *s2);
size_t ice_strlen (const char *string);
char *nice_time_minutes (unsigned long int minutes, char *buf);
const char *nullcheck_string (const char *string);
void clean_http_string(char *string);
#endif

/* vsnprintf.h. ajd *********************************************************/
#ifndef __ICE_VSNPRINTF_H__
#define __ICE_VSNPRINTF_H__

#ifndef HAVE_VSNPRINTF
int vsnprintf(char* s, int n, char* fmt, va_list stack);
#endif

#endif

/* vars.h. ajd *************************************************************/
void add_varpair (vartree_t *request_vars, char *varpair);
void add_varpair2 (vartree_t *request_vars, char *name, char *value);
const char *get_variable (vartree_t *request_vars, const char *name);
void free_variables (vartree_t *request_vars);
vartree_t *create_header_vars ();
void extract_header_vars (char *line, vartree_t *vars);
void free_con_variables (connection_t *con);
const char *get_con_variable (connection_t *con, const char *name);
