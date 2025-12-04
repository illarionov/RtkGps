/* ntrip_string.c
 * - Utilities to manipulate character data
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

#ifdef HAVE_ASSERT_H
#include <assert.h>
#endif
#include <string.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#include <stdlib.h>
#include <stdarg.h>
#include <sys/types.h>
#include <ctype.h>

#ifndef _WIN32
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#else
#include <winsock.h>
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "utility.h"
#include "ntrip_string.h"
#include "sock.h"
#include "log.h"

/* vars.c. ajd **********************************************************/
#ifndef __USE_BSD
#define __USE_BSD
#endif
#ifndef __EXTENSIONS__
#define __EXTENSIONS__
#endif
#include <time.h>
#ifdef _WIN32
#include <io.h>
#else
#include <dirent.h>
#endif


extern server_info_t info;
extern int running;
extern mutex_t authentication_mutex, library_mutex, sock_mutex;

const char const_null[] = "(null)";

char *
splitc (char *first, char *rest, const char divider)
{
	char *p;

	if (!rest)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: splitc called with NULL pointers");
		return NULL;
	}

	p = strchr(rest, divider);
	if (p == NULL) {
		if ((first != rest) && (first != NULL))
			first[0] = 0;

		return NULL;
	}

	*p = 0;
	if (first != NULL) strcpy(first, rest);
	if (first != rest) strcpy(rest, p + 1);

	return rest;
}

char *
clean_string(char *string)
{
	register unsigned i;

	i = 0;
	while (string[i] == ' ' && string[i] != '\0')
		i++;
	
	return &string[i];
}

const char *
con_host (connection_t *con)
{
	static char null[5] = "null";

	if (!con)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: con_host called with NULL connection");
		return null;
	}

	if (con->hostname)
		return con->hostname;
	else if (con->host)
		return con->host;
	else
		return null;
}

char *
my_strdup (const char *string)
{
	const char *ptr = string;
	if (!string)
	{
		xa_debug (1, "DEBUG: my_strdup called with NULL pointer!");
		return NULL;
	}
	while (ptr && *ptr && *ptr == ' ')
		ptr++;
	return nstrdup (ptr);
}

char alphabet[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

char unalphabet(char alpha)
{
	if (alpha >= 'A' && alpha <= 'Z')
		return (alpha - 'A');
	else if (alpha >= 'a' && alpha <= 'z')
		return (alpha - 'a' + 26);
	else if (alpha >= '0' && alpha <= '9')
		return (alpha - '0' + 52);
	else if (alpha == '+')
		return 62;
	else if (alpha == '/')
		return 63;
	else if (alpha == '=')
		return 64;
	else 
		return 65;
}

char *
util_base64_decode(char *message)
{
	char *decoded, temp;
	long length, decoded_length;
	long bitqueue, pad, i = 0, j = 0;

	length = ice_strlen(message);

	if (((length % 4) != 0) || (length == 0)) return NULL;

	decoded_length = length / 4 * 3;

	if (message[length - 1] == '=') {
		decoded_length--;
		if (message[length - 2] == '=')
			decoded_length--;
	}

	decoded = (char *)nmalloc(decoded_length + 1);
	memset (decoded, 0, decoded_length + 1);

	while (i < length) {
		pad = 0;

		temp = unalphabet(message[i++]);
		if (temp == 64) {
			free(decoded);
			return NULL;
		}
		bitqueue = temp;

		temp = unalphabet(message[i++]);
		if (temp == 64) {
			free(decoded);
			return NULL;
		}
		bitqueue <<= 6;
		bitqueue += temp;

		temp = unalphabet(message[i++]);
		if (temp == 64) {
			if (i != length - 1) {
				free(decoded);
				return NULL;
			}
			temp = 0; pad++;
		}
		bitqueue <<= 6;
		bitqueue += temp;

		temp = unalphabet(message[i++]);
		if (pad == 1 && temp != 64) {
				free(decoded);
				return NULL;
		}
		
		if (temp == 64) {
			if (i != length) {
				free(decoded);
				return NULL;
			}
			temp = 0; pad++;
		}
		bitqueue <<= 6;
		bitqueue += temp;

		decoded[j++] = ((bitqueue & 0xFF0000) >> 16);
		if (pad < 2) {
			decoded[j++] = ((bitqueue & 0xFF00) >> 8);
			if (pad < 1)
				decoded[j++] = (bitqueue & 0xFF);
		}
	}
	
	decoded[decoded_length] = '\0';

	return decoded;
}

char *
mutex_to_string (mutex_t *mutex, char *out)
{
	if (mutex == &info.source_mutex)
		strcpy (out, "Source Tree Mutex");
	else if (mutex == &info.misc_mutex)
		strcpy (out, "Misc. Mutex");
	else if (mutex == &info.mount_mutex)
		strcpy (out, "Mount Point Mutex");
	else if (mutex == &info.hostname_mutex)
		strcpy (out, "Hostname Tree Mutex");
	else if (mutex == &info.double_mutex)
		strcpy (out, "Double Mutex Mutex");
	else if (mutex == &info.thread_mutex)
		strcpy (out, "Thread Tree Mutex");
#ifdef DEBUG_MEMORY
	else if (mutex == &info.memory_mutex)
		strcpy (out, "Memory Tree Mutex");
#endif
#ifdef DEBUG_SOCKETS
	else if (mutex == &sock_mutex) {
		strcpy (out, "Socket Tree Mutex");
	}
#endif
	else if (mutex == &info.resolvmutex)
		strcpy (out, "DNS Lookup Mutex");
	else if (mutex == &library_mutex)
		strcpy (out, "Library Mutex");
	else if (mutex == &authentication_mutex)
		strcpy (out, "Authentication Mutex");
	else 
		strcpy (out, "Unknown Mutex (probably source)");
	return out;
}

char *create_malloced_ascii_host (struct in_addr *in)
{
	char *buf = (char *)nmalloc(20);

	if (!in) {
		xa_debug(1, "ERROR: Dammit, don't send NULL's to create_malloced_ascii_host()");
		return NULL;
	}

	return makeasciihost(in, buf);
}

char *makeasciihost(const struct in_addr *in, char *buf)
{
	if (!buf) {
		android_log(ANDROID_LOG_VERBOSE, "ERROR: makeasciihost called with NULL arguments");
		return NULL;
	}
  
#ifdef HAVE_INET_NTOA

	strncpy(buf, inet_ntoa(*in), 20);

#else

	unsigned char *s = (unsigned char *)in;
	int a, b, c, d;
	a = (int)*s++;
	b = (int)*s++;
	c = (int)*s++;
	d = (int)*s;

	snprintf(buf, 20, "%d.%d.%d.%d", a, b, c, d);

#endif

	return buf;
}

char *
nice_time_minutes (unsigned long int minutes, char *buf)
{
	unsigned long int days, hours, remains;
	
	if (!buf)
	{
		android_log (ANDROID_LOG_VERBOSE, "ERROR: nice_time_minutes called with NULL argument");
		return NULL;
	}

	buf[0] = '\0';

	days = minutes / 1440;
	remains = minutes % 1440;
	hours = remains / 60;
	remains = remains % 60;

	if (days > 0)
		snprintf(buf, BUFSIZE, "%lu days, %lu hours, %lu minutes", days, hours, remains);
	else if (hours > 0)
		snprintf(buf, BUFSIZE, "%lu hours, %lu minutes", hours, remains);
	else
	{
		snprintf(buf, BUFSIZE, "%lu minutes", remains);
		return buf;
	}
	return buf;
}

char *
nice_time (unsigned long int seconds, char *buf)
{
	unsigned long int days, hours, minutes, nseconds, remains;
	char buf2[BUFSIZE];
	
	if (!buf)
	{
		android_log (ANDROID_LOG_VERBOSE, "ERROR: nice_time called with NULL argument");
		return NULL;
	}

	buf[0] = '\0';

	days = seconds / 86400;
	remains = seconds % 86400;
	hours = remains / 3600;
	remains = remains % 3600;
	minutes = remains / 60;
	nseconds = remains % 60;
	if (days > 0)
		snprintf(buf, BUFSIZE, "%lu days, %lu hours, %lu minutes", days, hours, minutes);
	else if (hours > 0)
		snprintf(buf, BUFSIZE, "%lu hours, %lu minutes", hours, minutes);
	else if (minutes > 0)
		snprintf(buf, BUFSIZE, "%lu minutes", minutes);
	else
	{
		snprintf(buf, BUFSIZE, "%lu seconds", nseconds);
		return buf;
	}

	if (nseconds > 0)
	{
		snprintf(buf2, BUFSIZE, " and %lu seconds", nseconds);
		strncat(buf, buf2, BUFSIZE - strlen(buf2));
	}

	return buf;
}


size_t
ice_strlen (const char *string)
{
	if (!string)
	{
		xa_debug (1, "ERROR: ice_strlen() called with NULL pointer!");
		return 0;
	}
	return strlen (string);
}

int
ice_strcmp (const char *s1, const char *s2)
{
	if (!s1 || !s2)
	{
		xa_debug (1, "ERROR: ice_strcmp() called with NULL pointers!");
		return 0;
	}
	return strcmp (s1, s2);
}

int
ice_strncmp (const char *s1, const char *s2, size_t n)
{
	if (!s1 || !s2)
	{
		xa_debug (1, "ERROR: ice_strncmp() called with NULL pointers!");
		return 0;
	}
	return strncmp (s1, s2, n);
}

int
ice_strcasecmp (const char *s1, const char *s2)
{
	if (!s1 || !s2)
	{
		xa_debug (1, "ERROR: ice_strcasecmp() called with NULL pointers");
		return 0;
	}
#ifdef _WIN32
	return stricmp (s1, s2);
#else
	return strcasecmp (s1, s2);
#endif
}

const char *
nullcheck_string (const char *string)
{
	if (!string)
		return const_null;
	return string;
}

/* vsnprintf.c. ajd ***********************************************************************/

#ifndef HAVE_VSNPRINTF

int vsnprintf(char* s, int n, char* fmt, va_list stack)
{
	char *f, *sf = 0;
	int i, on, argl = 0;
	char myf[10], buf[20];
	char *arg, *myfp;

	on = n;
	f = fmt;
	arg = 0;
	while (arg || (sf = index(f, '%')) || (sf = f + strlen(f))) {
		if (arg == 0) {
			arg = f;
			argl = sf - f;
		}
		if (argl) {
			i = argl > n - 1 ? n - 1 : argl;
			strncpy(s, arg, i);
			s += i;
			n -= i;
			if (i < argl) {
				*s = 0;
				return on;
			}
		}
		arg = 0;
		if (sf == 0)
			continue;
		f = sf;
		sf = 0;
		if (!*f)
			break;
		myfp = myf;
		*myfp++ = *f++;
		while (((*f >= '0' && *f <='9') || *f == '#')
		       && myfp - myf < 8)
		{
			*myfp++ = *f++;
		}
		*myfp++ = *f;
		*myfp = 0;
		if (!*f++)
			break;
		switch(f[-1])
		{
		case '%':
			arg = "%";
			break;
		case 'c':
		case 'o':
		case 'd':
		case 'x':
			i = va_arg(stack, int);
			snprintf(buf, 20, myf, i);
			arg = buf;
			break;
		case 's':
			arg = va_arg(stack, char *);
			if (arg == 0)
				arg = "NULL";
			break;
		default:
			arg = "";
			break;
		}
		argl = strlen(arg);
	}
	*s = 0;
	return on - n;

	va_end(stack);
}
#endif


/* vars.c. ajd **********************************************************************/

vartree_t *
create_header_vars ()
{
	avl_tree *t = avl_create (compare_vars, &info);
	return t;
}

void
extract_header_vars (char *line, vartree_t *vars)
{
	char *colonptr;
	char name[BUFSIZE];
		
	if (!line || !vars)
	{
		xa_debug (1, "ERROR: extract_header_vars() called with NULL pointers");
		return;
	}

	colonptr = strchr (line, ':');
	if (!colonptr && line[0])
	{
		xa_debug (1, "WARNING: Invalid header line [%s] without colon", line);
		return;
	}
	
	if (splitc (name, line, ':') == NULL)
	{
		if (line[0])
			xa_debug (1, "WARNING: Invalid header line [%s]", line);
		return;
	}

	add_varpair2 (vars, nstrdup (clean_string (name)), nstrdup (clean_string (line)));
}

varpair_t *
create_varpair ()
{
	varpair_t *vp = (varpair_t *) nmalloc (sizeof (varpair_t));
	return vp;
}

void
add_varpair2 (vartree_t *request_vars, char *name, char *value)
{
  varpair_t *vp, *out;

  if (!request_vars)
    {
      xa_debug (2, "add_varpair2() called with NULL tree");
      return;
    }
  else if (!name || !value)
    {
      xa_debug (2, "add_varpair2() called with NULL values");
      return;
    }

  vp = create_varpair ();
  vp->name = name;
  vp->value = value;

  xa_debug (3, "DEBUG: Adding varpair [%s] == [%s]", vp->name, vp->value);

  if (!vp->name || !vp->value)
    {
      xa_debug (1, "WARNING: Adding NULL variables to tree");
      return;
    }

  out = avl_replace (request_vars, vp);

  if (out)
    {
      nfree (out->name);
      nfree (out->value);
      nfree (out);
    }
}

void
add_varpair (vartree_t *request_vars, char *varpair)
{
  char name[BUFSIZE];

  if (!varpair)
    {
      xa_debug (2, "WARNING: add_varpair called with NULL input");
      return;
    }

  name[0] = '\0';

  if (splitc (name, varpair, '=') == NULL)
    {
      xa_debug (1, "WARNING: Invalid varpair [%s]", varpair);
      return;
    }

  add_varpair2 (request_vars, nstrdup (name), nstrdup (varpair));
}

const char *
get_con_variable (connection_t *con, const char *name)
{
	if (!con || !con->headervars)
		return NULL;

	return (get_variable (con->headervars, name));
}

const char *
get_variable (vartree_t *request_vars, const char *name)
{
	varpair_t search, *vp;
	
	if (!request_vars || !name)
	{
		xa_debug (2, "WARNING: get_variable called with NULL pointers");
		return NULL;
	}

	search.name = strchr (name, *name);

	vp = avl_find (request_vars, &search);
	if (!vp)
		return NULL;
	return vp->value;
}

void
free_con_variables (connection_t *con)
{
	if (!con)
		return;
	free_variables (con->headervars);
	con->headervars = NULL;
}

void
free_variables (vartree_t *request_vars)
{
	varpair_t *vp, *out;
	
	if (!request_vars)
	{
		xa_debug (2, "WARNING: free_variables called with NULL tree");
		return;
	}

	while ((vp = avl_get_any_node (request_vars)))
	{
		out = avl_delete (request_vars, vp);

		if (!out)
		{
			xa_debug (2, "DEBUG: Fishy stuff in free_variables.");
			continue;
		}

		nfree (out->name);
		nfree (out->value);
		nfree (out);
	}
	
	avl_destroy (request_vars, NULL);
}

