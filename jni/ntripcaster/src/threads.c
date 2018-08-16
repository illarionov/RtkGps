/* threads.c
 * - Thread Abstraction Functions
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
#include "win32config.h"
#else
#include "config.h"
#endif
#endif

#include "definitions.h"

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#ifndef __USE_BSD
#define __USE_BSD
#endif
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include <signal.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef HAVE_ASSERT_H
#include <assert.h>
#endif

#ifdef _WIN32
#include <windows.h>
# ifndef STACKSIZE
#  define STACKSIZE 8192
# endif
#else
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#endif

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "log.h"
#include "utility.h"
#include "ntrip_string.h"
#include "main.h"

/* memory.c. ajd ************************************************************************/
#ifndef __EXTENSIONS__
#define __EXTENSIONS__
#endif
#ifdef HAVE_MATH_H
#include <math.h>
#endif


extern server_info_t info;

mutex_t library_mutex = {MUTEX_STATE_UNINIT};

#ifdef DEBUG_MEMORY
void thread_mem_check(mythread_t *thread)
{
	avl_traverser trav = {0};
	meminfo_t *mt;

	thread_mutex_lock(&info.memory_mutex);

	while ((mt = avl_traverse(info.mem, &trav))) {
		if (mt->thread_id == thread->id)
			xa_debug(1, "WARNING: %d bytes allocated by thread %d at line %d in %s not freed before thread exit", mt->size, thread->id, mt->line, mt->file);
	}
	
	thread_mutex_unlock(&info.memory_mutex);
}
#endif

icethread_t thread_create_c(char *name, void *(*start_routine)(void *), void *arg, int line, char *file)
{
#ifndef _WIN32
	icethread_t thread;
	pthread_attr_t attr;
	int i;
#else
	HANDLE ret;
#endif
	mythread_t *mt = (mythread_t *)nmalloc(sizeof(mythread_t));

	mt->line = line;
	mt->file = nstrdup(file);

	internal_lock_mutex (&info.thread_mutex);	
	mt->id = thread_new();
	internal_unlock_mutex (&info.thread_mutex);

	mt->name = nstrdup(name);
	mt->created = get_time();
	mt->ping = 0;
	mt->running = THREAD_CREATED;

#ifdef _WIN32
	ret = CreateThread(NULL, STACKSIZE, (LPTHREAD_START_ROUTINE)start_routine, arg, 0, &mt->thread);
#else
	pthread_attr_init(&attr);

#ifdef HAVE_PTHREAD_ATTR_SETSTACKSIZE
	pthread_attr_setstacksize(&attr, 1024*250);
#endif

	for (i = 0; i < 10; i++) {
# ifdef hpux
		if (pthread_create ((pthread_t *) &thread, pthread_attr_default,
				    (pthread_startroutine_t) start_routine,
				    (pthread_addr_t) arg) == 0)
# else
	        if (pthread_create(&thread, &attr, start_routine, arg) == 0)
# endif
			break;
		else
			android_log(ANDROID_LOG_VERBOSE, "ERROR: Could not create new thread, retrying");
	}

	pthread_attr_destroy(&attr);	
#endif

#ifdef _WIN32
	if (ret == NULL) {
#else
	if (i >= 10) {
#endif
		android_log(ANDROID_LOG_VERBOSE, "System won't let me create more threads, giving up");
		clean_shutdown(&info);
	}

#ifndef _WIN32
	mt->thread = thread;
#endif

	internal_lock_mutex(&info.thread_mutex);
	if (avl_insert(info.threads, mt))
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: Inserting thread resulted in duplicate.. sheit!");
	}
	internal_unlock_mutex(&info.thread_mutex);

        xa_debug (3, "DEBUG: Adding thread %d started at [%s:%d]", mt->id, file, line);
	
#ifndef _WIN32
# ifdef hpux
	pthread_detach((pthread_t *) &thread);
# else
	pthread_detach(thread);
# endif
#endif

	return mt->thread;
}

void
thread_create_mutex_nl (mutex_t *mutex)
{
	if (!mutex) {
		android_log(ANDROID_LOG_ERROR, "WARNING: thread_create_mutex_nl() called with NULL mutex\n");
		return;
	}

	mutex->thread_id = MUTEX_STATE_NEVERLOCKED;
	mutex->lineno = -1;
#ifdef _WIN32
	InitializeCriticalSection(&mutex->mutex);
#else
# ifdef hpux
	pthread_mutex_init (&mutex->mutex, pthread_mutexattr_default);
# else
#  if defined(DEBUG_MUTEXES) && defined (PHTREAD_ERRORCHECK_MUTEX_INITIALIZER_NP)
	mutex->mutex = PTHREAD_ERRORCHECK_MUTEX_INITIALIZER_NP;
#  else
	pthread_mutex_init(&mutex->mutex, NULL);
#  endif
# endif
#endif
}

void 
thread_create_mutex_c (mutex_t *mutex, int line, char *file)
{
	if (!mutex || !file) {
		android_log(ANDROID_LOG_ERROR, "WARNING: thread_create_mutex_c() called with NULL mutex\n");
		return;
	}

	mutex->thread_id = MUTEX_STATE_NEVERLOCKED;
	mutex->lineno = -1;
#ifdef _WIN32
	InitializeCriticalSection(&mutex->mutex);
#else
# ifdef hpux
	pthread_mutex_init (&mutex->mutex, pthread_mutexattr_default);
# else
#  if defined (DEBUG_MUTEXES) && defined (PHTREAD_ERRORCHECK_MUTEX_INITIALIZER_NP)	
	mutex->mutex = PTHREAD_ERRORCHECK_MUTEX_INITIALIZER_NP;
#  else
	pthread_mutex_init(&mutex->mutex, NULL);
#  endif
# endif
#endif

#ifdef DEBUG_MUTEXES
	internal_lock_mutex (&info.mutex_mutex);
	mutex->mutexid = thread_mutex_new ();
	avl_insert (info.mutexes, mutex);
	internal_unlock_mutex (&info.mutex_mutex);
#endif
}

void 
thread_mutex_destroy (mutex_t *mutex)
{

#ifdef _WIN32
	DeleteCriticalSection(&mutex->mutex);
#else
	pthread_mutex_destroy(&mutex->mutex);
#endif

#ifdef DEBUG_MUTEXES
	internal_lock_mutex (&info.mutex_mutex);
	avl_delete (info.mutexes, mutex);
	internal_unlock_mutex (&info.mutex_mutex);
#endif
}

void 
thread_mutex_lock_c (mutex_t *mutex, int line, char *file)
{
#ifdef OPTIMIZE
	internal_lock_mutex(mutex);
	return;
#else

# ifndef SAVE_CPU
	mythread_t *mt = thread_get_mythread();
	char name[40];

	if (!mt)
		android_log (ANDROID_LOG_VERBOSE, "WARNING: No mt record for %u in lock [%s:%d]", thread_self (), file, line);
	xa_debug (5, "Locking %p (%s) on line %d in file %s by thread %d", mutex, mutex_to_string(mutex, name), line, file, mt ? mt->id : -1);

# endif

# ifdef DEBUG_MUTEXES
	if (mt)
	{
		int locks = 0;
		avl_traverser trav = {0};
		mutex_t *tmutex;
		internal_lock_mutex (&info.mutex_mutex);

		while ((tmutex = avl_traverse (info.mutexes, &trav)))
		{
			if (tmutex->mutexid == mutex->mutexid) /* This mutex */
			{
				if (tmutex->thread_id == mt->id) /* Deadlock, same thread can't lock the same mutex twice */
				{
					android_log (ANDROID_LOG_VERBOSE, "DEADLOCK AVOIDED (%d == %d) on mutex [%s] in file %s line %d by thread %d [%s]", 
						   tmutex->thread_id, mt->id, mutex_to_string (mutex, name), file, line, mt->id, mt->name);
					internal_unlock_mutex (&info.mutex_mutex);
					return;
				}
			} else if (tmutex->thread_id == mt->id) /* Mutex locked by this thread (not this mutex) */
				locks++;
		}

		if (locks > 0) /* Has already got a mutex locked */
		{
			if (info.double_mutex.thread_id != mt->id) /* Tries to lock two mutexes, but has not got the double mutex */
			{
				android_log (ANDROID_LOG_VERBOSE, "WARNING: (%d != %d) Thread %d [%s] tries to lock a second mutex [%s] in file %s line %d, without locking double mutex!",
					   info.double_mutex.thread_id, mt->id, mt->id, mt->name, mutex_to_string (mutex, name), file, line);
			}
		}
		internal_unlock_mutex (&info.mutex_mutex);
	}
# endif
internal_lock_mutex(mutex);


# ifndef SAVE_CPU
#  ifdef DEBUG_MUTEXES
	internal_lock_mutex (&info.mutex_mutex);
#  endif
	xa_debug (5, "Locked %p by thread %d", mutex, mt ? mt->id : -1);
	mutex->lineno = line;
	if (mt)
		mutex->thread_id = mt->id;
#  ifdef DEBUG_MUTEXES
	internal_unlock_mutex (&info.mutex_mutex);
#  endif
# endif

#endif
}

void 
thread_mutex_unlock_c(mutex_t *mutex, int line, char *file)
{
#ifdef OPTIMIZE
	internal_unlock_mutex(mutex);
	return;
#else
	
# ifndef SAVE_CPU
	mythread_t *mt = thread_get_mythread();
	char name[40];
	if (!mt)
		android_log (ANDROID_LOG_VERBOSE, "WARNING: No mt record for %u in unlock [%s:%d]", thread_self (), file, line);
	xa_debug(5, "Unlocking %p (%s) on line %d in file %s by thread %d", mutex, mutex_to_string (mutex, name), line, file, mt ? mt->id : -1);

	mutex->lineno = line;
# endif

# ifdef DEBUG_MUTEXES
	if (mt)
	{
		int locks = 0;
		avl_traverser trav = {0};
		mutex_t *tmutex;
		internal_lock_mutex (&info.mutex_mutex);
		while ((tmutex = avl_traverse (info.mutexes, &trav)))
		{
			if (tmutex->mutexid == mutex->mutexid) /* This mutex */
			{
				if (tmutex->thread_id != mt->id) /* Unlocking when it's not ours */
				{
					android_log (ANDROID_LOG_VERBOSE, "ILLEGAL UNLOCK (%d != %d) on mutex [%s] in file %s line %d by thread %d [%s]", tmutex->thread_id, mt->id, 
						   mutex_to_string (mutex, name), file, line, mt->id, mt->name);
					internal_unlock_mutex (&info.mutex_mutex);
					return;
				}
			} else if (tmutex->thread_id == mt->id)
				locks++;
		}

		if ((locks > 0) && (info.double_mutex.thread_id != mt->id)) /* Don't have double mutex, has more than this mutex left */
		{
			android_log (ANDROID_LOG_VERBOSE, "WARNING: (%d != %d) Thread %d [%s] tries to unlock a mutex [%s] in file %s line %d, without owning double mutex!",
				   info.double_mutex.thread_id, mt->id, mt->id, mt->name, mutex_to_string (mutex, name), file, line);
		}
		internal_unlock_mutex (&info.mutex_mutex);
	}
# endif

	internal_unlock_mutex(mutex);

# ifndef SAVE_CPU
#  ifdef DEBUG_MUTEXES
	internal_lock_mutex (&info.mutex_mutex);
#  endif
	xa_debug (5, "Unlocked %p by thread %d", mutex, mt ? mt->id : -1);
	mutex->lineno = -1;
	if (mt && mutex->thread_id == mt->id)
		mutex->thread_id = MUTEX_STATE_NOTLOCKED;
#  ifdef DEBUG_MUTEXES
	internal_unlock_mutex (&info.mutex_mutex);
#  endif
# endif 
#endif
}

void 
thread_exit_c (int val, int line, char *file)
{
	mythread_t *out, *mt = thread_get_mythread();

#ifdef DEBUG_MUTEXES
	if (mt)
	{
		avl_traverser trav = {0};
		mutex_t *tmutex;
		char name[40];

		mt->running = THREAD_EXITED;

		internal_lock_mutex (&info.mutex_mutex);
		while ((tmutex = avl_traverse (info.mutexes, &trav)))
			if (tmutex->thread_id == mt->id)
				android_log (ANDROID_LOG_VERBOSE, "WARNING: Thread %d [%s] exiting in file %s line %d, without unlocking mutex [%s]",
					   mt->id, mt->name, file, line, mutex_to_string (tmutex, name));
		internal_unlock_mutex (&info.mutex_mutex);
	}
#endif
	
	if (mt)	{
		xa_debug(2, "DEBUG: Removing thread %d started at [%s:%d], reason: 'Thread Exited'", mt->id, mt->file, mt->line);

		internal_lock_mutex(&info.thread_mutex);
		out = avl_delete (info.threads, mt);
		internal_unlock_mutex(&info.thread_mutex);

		if (out) {
			if (out->id == 0)
			{
				if (out->file)
					free (out->file);
				if (out->name)
					free (out->name);
				if (out)
					free (out);
			} else {
#ifdef DEBUG_MEMORY
				thread_mem_check(mt);
#endif
				nfree(out->file);
				nfree(out->name);
				nfree(out);
			}
		}
	}
	
#ifdef _WIN32
	ExitThread(val);
#else
	pthread_exit((void *)val);
#endif
}

/* 
 * Signals should be handled by the main thread.
 */
void
thread_block_signals ()
{
#if defined(HAVE_SIGACTION) && defined(HAVE_PTHREAD_SIGMASK)
	sigset_t ss;

	sigfillset (&ss);

	sigdelset (&ss, SIGKILL);
	sigdelset (&ss, SIGSTOP);
	sigdelset (&ss, SIGTERM);
	sigdelset (&ss, SIGSEGV);
	sigdelset (&ss, SIGBUS);
	if (pthread_sigmask (SIG_BLOCK, &ss, NULL) != 0) {
#ifdef DEBUG_FULL
		android_log (ANDROID_LOG_VERBOSE, "WARNING: pthread_sigmask() failed!");
#endif
	}
#endif
}

/* 
 * Let the calling thread catch all the relevant signals
 */
void
thread_catch_signals ()
{
#if defined(HAVE_SIGACTION) && defined(HAVE_PTHREAD_SIGMASK)
	sigset_t ss;
	
	sigemptyset (&ss);

	/* These ones should only be accepted by the signal handling thread (main thread) */
	sigaddset (&ss, SIGHUP);
	sigaddset (&ss, SIGCHLD);
	sigaddset (&ss, SIGINT);

#ifdef SIGPIPE
	sigaddset (&ss, SIGPIPE);
#endif
	
	if (pthread_sigmask (SIG_UNBLOCK, &ss, NULL) != 0)
		android_log (ANDROID_LOG_VERBOSE, "WARNING: pthread_sigmask() failed!");
#endif
}

void 
thread_init()
{
	mythread_t *mt = thread_check_created ();
	int max = 600;

	thread_block_signals ();

	while (!mt && (max > 0)) /* Not inserted in the thread tree yet */
	{
		my_sleep (40000);
		mt = thread_check_created ();
		max--;
	}
	
	if (max == 0 || !mt)
	{
		log_no_thread (1, "DEBUG: Thread never made it to life.");
		thread_exit (13);
	}

	if (mt)
		mt->running = THREAD_RUNNING;
}

icethread_t thread_self()
{
#ifdef _WIN32
    return GetCurrentThreadId();
#else
    return pthread_self();
#endif
}

int 
thread_equal(icethread_t t1, icethread_t t2)
{
#ifdef _WIN32
    return (t1 == t2) ? 1 : 0;
#else
    return pthread_equal(t1, t2);
#endif
}

long 
thread_new()
{
	info.threadid++;
	return info.threadid;
}

long 
thread_mutex_new ()
{
	info.mutexid++;
	return info.mutexid;
}

mythread_t *
thread_get_mythread()
{
	avl_traverser trav = {0};
	mythread_t *mt;
	icethread_t t = thread_self ();

	internal_lock_mutex(&info.thread_mutex);

	if (info.threads == NULL)
	{
		fprintf (stderr, "WARNING: Thread tree is empty, this must be wrong!");
		internal_unlock_mutex (&info.thread_mutex);
		return NULL;
	}
	
	while ((mt = avl_traverse(info.threads, &trav))) {
		if (thread_equal(t, mt->thread)) {
			internal_unlock_mutex(&info.thread_mutex);
			return mt;
		}
	}
	internal_unlock_mutex (&info.thread_mutex);
	android_log (ANDROID_LOG_VERBOSE, "WARNING: Nonexistant thread alive...");
	return NULL;
}

mythread_t *
thread_check_created()
{
	avl_traverser trav = {0};
	mythread_t *mt;
	icethread_t t = thread_self ();

	if (info.threads == NULL)
	{
		fprintf (stderr, "WARNING: Thread tree is empty, this must be wrong!");
		return NULL;
	}

	internal_lock_mutex(&info.thread_mutex);
	
	while ((mt = avl_traverse(info.threads, &trav))) {
		if (thread_equal(t, mt->thread)) {
			internal_unlock_mutex(&info.thread_mutex);
			return mt;
		}
	}
	internal_unlock_mutex (&info.thread_mutex);
	return NULL;
}

void 
thread_rename(const char *name)
{
	mythread_t *mt;

	if (info.threads == NULL) return;
	
	mt = thread_get_mythread ();
	if (mt->name) 
	{
		nfree(mt->name);
	}
	mt->name = nstrdup(name);
}

void internal_lock_mutex(mutex_t *mutex)
{
	if (!mutex) {
		android_log(ANDROID_LOG_ERROR, "ERROR: internal_lock_mutex() called with NULL pointer!");
	}

#ifdef _WIN32
	EnterCriticalSection(&mutex->mutex);
#else
	switch (pthread_mutex_lock(&mutex->mutex)) {
		case EINVAL:
			android_log(ANDROID_LOG_ERROR, "WARNING: Locking unitialized mutex\n");
			break;
		case EDEADLK:
			android_log(ANDROID_LOG_ERROR, "WARNING: Locking mutex failed because a DEADLOCK would occur\n");
			break;
	}
#endif

}

void internal_unlock_mutex(mutex_t *mutex)
{
#ifdef _WIN32
	LeaveCriticalSection(&mutex->mutex);
#else
	switch (pthread_mutex_unlock(&mutex->mutex)) {
		case EINVAL:
			android_log(ANDROID_LOG_ERROR, "WARNING: Unlocking unitialized mutex\n");
			break;
# if defined(DEBUG_MUTEXES) && defined (EPERM)
		case EPERM:
			android_log(ANDROID_LOG_ERROR, "WARNING: Unlocking mutex failed because thread was not owner\n");
			break;
# endif
	}
#endif
}

void thread_lib_init()
{
	info.mutexes = NULL;
	info.mutexid = 0;

	info.mutexes = avl_create_nl (compare_mutexes, &info);
	thread_create_mutex_nl (&info.mutex_mutex);
	thread_create_mutex (&library_mutex);	
}

void thread_library_lock()
{
	/* make sure thread_lib_init() was called! */
	assert(library_mutex.thread_id != MUTEX_STATE_UNINIT); 
	internal_lock_mutex(&library_mutex);
}

void thread_library_unlock()
{
	internal_unlock_mutex(&library_mutex);
}

void
thread_setup_default_attributes ()
{
#if !defined(_WIN32) && defined(PTHREAD_CREATE_DETACHED)
	if (pthread_attr_init (&info.defaultattr) != 0) 
		android_log(ANDROID_LOG_ERROR, "WARNING: pthread_attr_init() failed!\n");
	if (pthread_attr_setdetachstate (&info.defaultattr, PTHREAD_CREATE_DETACHED) != 0)
		android_log(ANDROID_LOG_ERROR, "WARNING: pthread_attr_setdetachstate() failed!\n");
#endif
}

int
thread_alive (mythread_t *mt)
{
	if (mt->running != THREAD_RUNNING)
		return 0;
	return 1;
}

void
thread_wait_for_solitude ()
{
	int max = 300;
	
	if (avl_count (info.threads) <= 1)
		return;
	
	android_log (ANDROID_LOG_VERBOSE, "Waiting a wee while to let the other threads die..");
	
	do {

		if (avl_count (info.threads) <= 1) {
			android_log (ANDROID_LOG_VERBOSE, "Finally alone");
			return;
		}

		max--;
		
		my_sleep (30000);
	} while (max >= 0);
	
	android_log (ANDROID_LOG_VERBOSE, "Ok, that's enough, let's kill the remaining %d %s", avl_count (info.threads) - 1, avl_count (info.threads) > 2 ? "buggers" : "bugger");
}

/* memory.c. ajd ***************************************************************************/

#ifdef HAVE_MCHECK

void
icecast_mcheck_status (enum mcheck_status STATUS)
{
	fprintf (stderr, "WARNING MEMORY INTEGRITY COMPRIMISED!!!\n");
	switch (STATUS)
	{
		case MCHECK_HEAD:
			fprintf (stderr, "MCHECK_HEAD (pointer decremented to far)\n");
			break;
		case MCHECK_TAIL:
			fprintf (stderr, "MCHECK_TAIL (pointer incremented to far)\n");
			break;
		case MCHECK_FREE:
			fprintf (stderr, "MCHECK_FREE (block already free)\n");
			break;
		default:
			fprintf (stderr, "Unknown mcheck status\n");
			break;
	}
}

#endif

#ifdef DEBUG_MEMORY
/*
 * Create a dynamic memory info struct by calling malloc()
 * Set default values and return the created meminfo_t
 * Assert Class: 1
 */
meminfo_t *
create_meminfo ()
{
	meminfo_t *out = (meminfo_t *) malloc (sizeof (meminfo_t));
	if (!out)
	  return NULL;
	out->ptr = NULL;
	out->file[0] = '\0';
	out->line = -1;
	out->thread_id = -1;
	out->size = -1;
	out->time = -1;
	return out;
}
#endif

/*
 * Dynamically allocate size bytes and return the chunk
 * Assert Class: 1
 */
void *
n_malloc (const unsigned int size, const int lineno, const char *file)
{
	void *buf;

	if (size <= 0)
	{
		android_log(ANDROID_LOG_ERROR, "WARNING: n_malloc called with negative or zero size\n");
		return NULL;
	}

	buf = malloc (size);

	if (buf == NULL) {
		android_log(ANDROID_LOG_ERROR, "OUCH, out of memory!");
		clean_shutdown (&info);
	}
	
	if (size <= 0) {
		android_log(ANDROID_LOG_ERROR, "WARNING - Tried to allocate zero or negative size");
		return NULL;
	}

#ifdef DEBUG_MEMORY
	{
		meminfo_t *mi;
		mythread_t *mt = thread_get_mythread ();
		mi = create_meminfo();

		if (!mi)
			return buf;

		mi->line = lineno;
		strncpy (mi->file, file ? file : "unknown", 19);
		mi->ptr = buf;
		mi->time = get_time ();
		mi->size = size;
		if (mt)
			mi->thread_id = mt->id;
		internal_lock_mutex (&info.memory_mutex);
		avl_insert (info.mem, mi);
		internal_unlock_mutex (&info.memory_mutex);
	}
#endif
	return buf;
}

/*
 * Create a dynamically allocated string with the same data as ptr
 * Assert Class: 1
 */
char *
n_strdup (const char *ptr, const int lineno, const char *file)
{
	char *buf;

	if (!ptr)
	{
		ptr = "(null)";
	}

	buf = strdup (ptr);
#ifdef DEBUG_MEMORY
	{
		meminfo_t *mi;
		mythread_t *mt = thread_get_mythread();
		mi = create_meminfo();
		mi->line = lineno;
		strncpy(mi->file, file, 19);
		mi->ptr = buf;
		mi->size = ice_strlen (ptr) + 1;
		mi->time = get_time ();
		if (mt)
			mi->thread_id = mt->id;
		internal_lock_mutex (&info.memory_mutex);
		avl_insert(info.mem, mi);
		internal_unlock_mutex (&info.memory_mutex);
	}
#endif

	return buf;
}
		
/*
 * free the memory chunk pointed to by ptr
 * Assert Class: 1
 */
void
n_free (void *ptr, const int lineno, const char *file)
{
#ifdef DEBUG_MEMORY
	meminfo_t search, *out;
	search.ptr = ptr;
	internal_lock_mutex (&info.memory_mutex);
	out = avl_delete (info.mem, &search);
	internal_unlock_mutex (&info.memory_mutex);
	
	if (!out && ptr)
	{
		android_log (ANDROID_LOG_VERBOSE, "Couldn't find alloced memory at (%p)", ptr);
		return;
	}
	
	if (out)
		free (out);
#endif
	
	if (ptr)
		free (ptr);
}

char *
ice_cat (const char *first, const char *second)
{
  size_t sz = ice_strlen(first) + ice_strlen(second) + 1;
  char *res = (char *)nmalloc(sz);
  snprintf(res, sz, "%s%s", first, second);
  return res;
}

int
bytes_for (int bytes)
{
  return bytes * (int)(8 * (log(2) / (log(10)))) + 2;
}

void
initialize_memory_checker ()
{
#if defined(DEBUG_MEMORY_MCHECK) && defined(HAVE_MCHECK)
	mcheck (icecast_mcheck_status);
	mtrace();
	fprintf (stderr, "DEBUG: Starting memory checker\n");
#endif
}

//NEW
#define PTHREAD_JOIN_POLL_INTERVAL 10
#define false 0
#define true (!false)

typedef struct _waitData waitData;

struct _waitData
{
  pthread_t waitID;
  pthread_t helpID;
  int done;
};

void
sleep_msecs(int msecs)
{
  struct timeval tv;

  tv.tv_sec = msecs/1000;
  tv.tv_usec = (msecs % 1000) * 1000;
  select (0,NULL,NULL,NULL,&tv);
}
unsigned int
get_ticks()
{
  struct timeval tv;

  gettimeofday(&tv, NULL);
  return (tv.tv_usec/1000 + tv.tv_sec * 1000);
}

void thread_exit_handler(int sig)
{
	android_log(ANDROID_LOG_VERBOSE, "Thread cancelling...");
	pthread_exit(0);
}

void *
join_timeout_helper(void *arg)
{

	struct sigaction actions;
	 	memset(&actions, 0, sizeof(actions));
	 	sigemptyset(&actions.sa_mask);
	 	actions.sa_flags = 0;
	 	actions.sa_handler = thread_exit_handler;
	 	sigaction(SIGUSR1,&actions,NULL);
  waitData *data = (waitData*)arg;

  pthread_join(data->waitID, NULL);
  data->done = true;
  return (void *)0;
}

int
pthread_join_timeout(pthread_t wid, int msecs)
{
  pthread_t id;
  waitData data;
  unsigned int start = get_ticks();
  int timedOut = false;

  data.waitID = wid;
  data.done = false;

  if (pthread_create(&id, NULL, join_timeout_helper, &data) != 0)
    return (-1);
  do {
    if (data.done)
      break;
    /* you could also yield to your message loop here... */
    sleep_msecs(PTHREAD_JOIN_POLL_INTERVAL);
  } while ((get_ticks() - start) < msecs);
  if (!data.done)
  {
	pthread_kill(id, SIGUSR1);
    timedOut = true;
  }
  /* free helper thread resources */
  pthread_join(id, NULL);
  return (timedOut);
}
