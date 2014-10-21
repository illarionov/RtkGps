/* threads.h
 * - Thread Abstraction Function Headers
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

#ifndef __ICECAST_THREADS_H
#define __ICECAST_THREADS_H

#ifdef HAVE_PTHREAD_H
#   include <pthread.h>
#endif 
typedef pthread_t icethread_t;

typedef struct icemutex_St
{
	long int thread_id;
#ifndef _WIN32
	pthread_mutex_t mutex;
#else
	win32_mutex_t mutex;
#endif
	long int mutexid;
	int lineno;
	long int id;
} mutex_t;


#define thread_create(n,x,y) thread_create_c (n,x,y,__LINE__,__FILE__);
#define thread_create_mutex(x) thread_create_mutex_c (x,__LINE__,__FILE__);
#define thread_mutex_lock(x) thread_mutex_lock_c (x,__LINE__,__FILE__);
#define thread_mutex_unlock(x) thread_mutex_unlock_c (x,__LINE__,__FILE__);
#define thread_exit(x) thread_exit_c (x,__LINE__,__FILE__);

#define MUTEX_STATE_NOTLOCKED   -1
#define MUTEX_STATE_NEVERLOCKED -2
#define MUTEX_STATE_UNINIT -3

#define THREAD_CREATED -1
#define THREAD_RUNNING 1
#define THREAD_KILLED -2
#define THREAD_EXITED -3

typedef struct mythread_st
{
	icethread_t thread;
	int line;
	char *file;
	char *name;
	long int id;
	time_t created;
	int ping;
	int running;
} mythread_t;

void thread_lib_init();
icethread_t thread_create_c(char *name, void *(*start_routine)(void *), void *arg, int line, char *file);
void thread_create_mutex_c(mutex_t *mutex, int line, char *file);
void thread_mutex_lock_c(mutex_t *mutex, int line, char *file);
void thread_mutex_unlock_c(mutex_t *mutex, int line, char *file);
void thread_mutex_destroy (mutex_t *mutex);
void thread_exit_c(int val, int line, char *file);
void internal_lock_mutex(mutex_t *mutex);
void internal_unlock_mutex(mutex_t *mutex);

/*for using un-threadsafe library functions*/
void thread_library_lock();
void thread_library_unlock();
#define PROTECT_CODE(code) {thread_library_lock(); code ; thread_library_unlock();}

void thread_init();
void thread_wait_for_solitude ();

int thread_alive (mythread_t *mt);
void thread_block_signals();
void thread_catch_signals();
void thread_setup_default_attributes ();
icethread_t thread_self();
int thread_equal(icethread_t t1, icethread_t t2);
long thread_new();
long thread_mutex_new ();
void thread_create_mutex_nl (mutex_t *mutex);
mythread_t *thread_get_mythread();
mythread_t *thread_check_created();
void thread_mem_check (mythread_t *mt);
void thread_rename(const char *name); /* renames current thread */

#endif


/* memory.h. ajd ****************************************************************************/
#ifndef __ICECAST_MEMORY_H
#define __ICECAST_MEMORY_H

#ifdef HAVE_MCHECK_H
#include <mcheck.h>
#endif

#ifdef HAVE_MALLOC_H
#include <malloc.h>
#endif

#define nmalloc(x) n_malloc (x, __LINE__,__FILE__)
#define nfree(x) n_free (x,__LINE__,__FILE__) ; x=NULL
#define nstrdup(x) n_strdup (x,__LINE__,__FILE__)

typedef struct meminfo_t
{
	int line;
	int size;
	char file[20];
	void *ptr;
	int thread_id;
	time_t time;
} meminfo_t;

void *n_malloc (const unsigned int size, const int lineno, const char *file);
void n_free (void *ptr, const int lineno, const char *file);
char *n_strdup (const char *ptr, const int lineno, const char *file);
char *ice_cat (const char *first, const char *second);

void initialize_memory_checker ();

#ifdef HAVE_MCHECK_H
void icecast_mcheck_status (enum mcheck_status STATUS);
#endif

#endif
int pthread_join_timeout(pthread_t wid, int msecs);
