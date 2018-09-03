/* log.c
 * - Logging Functions
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
#include <sys/types.h>
#include <stdlib.h>
#include <errno.h>
#include <stdarg.h>
#include <string.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef _WIN32
#include <io.h>
#define write _write
#define read _read
#define close _close
#else
#include <sys/socket.h>
#include <netinet/in.h>
#endif

#include <fcntl.h>

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "log.h"
#include "sock.h"
#include "utility.h"
#include "ntrip_string.h"
#include "timer.h"
#include "main.h"
#include "connection.h"
#include "client.h"

/* logtime.c. ajd **********************************************************/

#ifdef _WIN32
# include <time.h>
# include <windows.h>
#else
# include <time.h>
# ifdef TIME_WITH_SYS_TIME
#  include <sys/time.h>
#endif
#endif


/* extern int errno, running; */
extern int running;
extern server_info_t info;


int
get_log_fd (int whichlog)
{
	if (info.logfile != -1)
		return info.logfile;
	
	return -1;
}


void 
write_log (int whichlog, char *fmt, ...)
{
	char buf[BUFSIZE];
	va_list ap;
	char *logtime;
	mythread_t *mt = thread_check_created ();
	int fd = get_log_fd (whichlog);


		va_start(ap, fmt);
		vsnprintf(buf, BUFSIZE, fmt, ap);
  
		if (!mt)
			fprintf (stderr, "WARNING: No mt while outputting [%s]", buf);

		logtime = get_log_time();

		if (strstr (buf, "%s") != NULL) {
			fprintf (stderr, "WARNING, write_log () called with '%%s' formatted string [%s]!", buf);
			free (logtime);
			return;
		}

		if (mt && fd != -1) {
			if ((whichlog != ANDROID_LOG_VERBOSE) || (info.logfiledebuglevel > -1)) {
				fd_write (fd, "[%s] [%d:%s] %s\n", logtime, mt->id, nullcheck_string (mt->name), buf);
			}
		}

		if (whichlog != ANDROID_LOG_VERBOSE)
		{
			free (logtime);
			va_end (ap);
			return;
		}

		if (running == SERVER_RUNNING) {
			printf("\r[%s] %s\n", logtime, buf);
			fflush(stdout);
		} else
			fprintf (stderr, "[%s] %s\n", logtime, buf);

		if (logtime)
			free(logtime);
		va_end (ap);
	
}

void 
log_no_thread (int whichlog, char *fmt, ...)
{
	char buf[BUFSIZE];
	va_list ap;
	char *logtime;
	int fd = get_log_fd (whichlog);


		va_start(ap, fmt);
		vsnprintf(buf, BUFSIZE, fmt, ap);
  
		logtime = get_log_time();

		if (strstr (buf, "%s") != NULL) {
			fprintf (stderr, "WARNING, write_log () called with '%%s' formatted string [%s]!", buf);
			logtime = get_log_time();
			return;
		}

		if (fd != -1) {
			if ((whichlog != ANDROID_LOG_VERBOSE) || (info.logfiledebuglevel > -1)) {
				fd_write (fd, "[%s] %s\n", logtime, buf);
			}
		}

		if (whichlog != ANDROID_LOG_VERBOSE)
		{
			free (logtime);
			va_end (ap);
			return;
		}

		if (running == SERVER_RUNNING) {
			printf("\r[%s] %s\n", logtime, buf);
			fflush(stdout);
		} else
			fprintf (stderr, "[%s] %s\n", logtime, buf);

		if (logtime)
			free(logtime);
		va_end (ap);

}

void 
xa_debug (int level, char *format, ...)
{
	//TODO
	if (level > 3) return;
	   va_list ap;

	   va_start(ap, format);
	   __android_log_vprint(ANDROID_LOG_VERBOSE, TAG, format, ap);
	   va_end(ap);
}

void android_log(int android_level, char *format, ...)
{
	va_list ap;
	va_start(ap, format);
	__android_log_vprint(ANDROID_LOG_INFO, TAG, format, ap);
	va_end(ap);
}

void open_log_files()
{
	info.logfile = open_log_file(info.logfilename, info.logfile);
}

int open_log_file(char *name, int oldfd)
{
	return 0;
}

int
fd_write (int fd, const char *fmt, ...)
{
	char buff[BUFSIZE];
	va_list ap;
	
	va_start(ap, fmt);
	vsnprintf(buff, BUFSIZE, fmt, ap);
	va_end (ap);
	
	if (fd == 1 || fd == 0) {
		if (running == SERVER_RUNNING) {
			fprintf(stdout, "%s", buff);
			fflush(stdout);
			return 1;
#ifndef _WIN32
		} else {
			return write(fd, buff, ice_strlen(buff));
		}
#else
		}	
#endif
        } else {
		return write(fd, buff, ice_strlen(buff));
	}
    return 0;
}

/*
 * Read one line of at max len bytes from sockfd into buff.
 * If ok, return 1 and nullterminate buff. Otherwise return 0.
 * Terminating \n is not put into the buffer.
 * Assert Class: 2
 */
int 
fd_read_line (int fd, char *buff, const int len)
{
	char c = '\0';
	int read_bytes, pos;

	buff[len-1] = ' ';

       if (!buff) {
          xa_debug (1, "ERROR: fd_read_line () called with NULL storage pointer");
          return 0;
	} else if (len <= 0) {
	  xa_debug (1, "ERROR: fd_read_line () called with invalid length");
	  return 0;
	}

	pos = 0;
	read_bytes = read (fd, &c, 1);

        if (read_bytes < 0)
	{
		xa_debug (1, "DEBUG: read error on file descriptor %d [%d]", fd, errno);
		return 0;
	}

	while ((c != '\n') && (pos < len) && (read_bytes == 1)) {
		if (c != '\r')
                     buff[pos++] = c;
		read_bytes = read (fd, &c, 1);
	}

	if (pos < len)
		buff[pos] = '\0';
	else {
		buff[len-1] = '\0';
		xa_debug(1, "ERROR: read line too long (exceeding BUFSIZE)");
		return 0;
	}

	return ((pos > 0) || (c == '\n')) ? 1 : 0;
}

int
fd_close (int fd)
{

	if (fd < 2)
		xa_debug (1, "DEBUG: Closing fd %d", fd);
	else
		xa_debug (1, "DEBUG: Closing fd %d", fd);

	if (fd >= 0) {
		return close (fd);
	}
	else
		return -1;
}

/* logtime.c. ajd ***********************************************************************/

long get_time()
{
	return time(NULL);
}

char *get_log_time()
{
	return get_string_time(get_time(), REGULAR_TIME);
}

char *get_string_time(time_t tt, char *format)
{
	char *buff;
	
	/* Don't set this to nmalloc */
	buff = (char *)malloc(40);
	memset(buff, 0, 40);
	
#ifdef HAVE_LOCALTIME_R
	{
		struct tm mt, *pmt;

		if (!(pmt = localtime_r(&tt, &mt))) {
			strcpy (buff, "error");
		} else {
			if (strftime(buff, 40, format, pmt) == 0)
				strcpy (buff, "error");
		}
	}
#else
	{
		struct tm *t;
		/* localtime is NOT threadsafe on all platforms */
		thread_library_lock();
		t = localtime(&tt);
		strftime(buff, 40, format, t);
		thread_library_unlock();
	}
#endif

	return buff;
}
