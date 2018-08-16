#include <android/log.h>
#define TAG "ntripcaster"

#define LOGWRITE(LOG, MESSAGE) __android_log_print(LOG,TAG, MESSAGE)
#define LOGVWRITE(LOG, FORMAT, ...) __android_log_print(LOG,TAG,FORMAT, __VA_ARGS__)

/* Define if on AIX 3.
   System headers sometimes define this.
   We just want to avoid a redefinition error message.  */
#ifndef _ALL_SOURCE
/* #undef _ALL_SOURCE */
#endif

/* Define to empty if the keyword does not work.  */
/* #undef const */

/* Define if you don't have vprintf but do have _doprnt.  */
/* #undef HAVE_DOPRNT */

/* Define if you have the strftime function.  */
#define HAVE_STRFTIME 1

/* Define if you have <sys/wait.h> that is POSIX.1 compatible.  */
#define HAVE_SYS_WAIT_H 1

/* Define if you have the vprintf function.  */
#define HAVE_VPRINTF 1

/* Define if on MINIX.  */
/* #undef _MINIX */

/* Define to `int' if <sys/types.h> doesn't define.  */
/* #undef pid_t */

/* Define if the system does not provide POSIX.1 features except
   with this defined.  */
/* #undef _POSIX_1_SOURCE */

/* Define if you need to in order for stat and other things to work.  */
/* #undef _POSIX_SOURCE */

/* Define as the return type of signal handlers (int or void).  */
#define RETSIGTYPE void

/* Define to `unsigned' if <sys/types.h> doesn't define.  */
/* #undef size_t */

/* Define if you have the ANSI C header files.  */
#define STDC_HEADERS 1

/* Define on System V Release 4.  */
/* #undef SVR4 */

/* Define if you can safely include both <sys/time.h> and <time.h>.  */
#define TIME_WITH_SYS_TIME 1

/* Define if your <sys/time.h> declares struct tm.  */
/* #undef TM_IN_SYS_TIME */

/* Whether to use crypted passwords */
/* #undef USE_CRYPT */

/* Whether to use tcp_wrappers */
/* #undef HAVE_LIBWRAP */

/* Some systems have sys/syslog.h */
/* #undef NEED_SYS_SYSLOG_H */

/* We might be the silly hpux */
/* #undef hpux */

/* Are we sysv? */
/* #undef SYSV */

/* Fucked up IRIX */
/* #undef IRIX */

/* Or svr4 perhaps? */
/* #undef SVR4 */

/* Some kind of Linux */
/* #undef LINUX */

/* Or perhaps some bsd variant? */
/* #undef __SOMEBSD__ */

/* UNIX98 and others want socklen_t */
/* #undef HAVE_SOCKLEN_T */

/* The complete version of icecast */
#define VERSION "0.1.5"

/* Definately Solaris */
/* #undef SOLARIS */

/* directories that we use... blah blah blah */
/* #undef LITTLECASTER_ETCDIR */
/* #undef LITTLECASTER_LOGDIR */
/* #undef LITTLECASTER_TEMPLATEDIR */

/* DAMN I HATE HATE HATE AUTOCONF */
#define HAVE_SOCKET 1
#define HAVE_CONNECT 1
#define HAVE_GETHOSTBYNAME 1
#define HAVE_NANOSLEEP 1
/* #undef HAVE_YP_GET_DEFAULT_DOMAIN */

/* Define if you have the basename function.  */
#define HAVE_BASENAME 1

/* Define if you have the connect function.  */
#define HAVE_CONNECT 1

/* Define if you have the gethostbyaddr_r function.  */
/* #undef HAVE_GETHOSTBYADDR_R */

/* Define if you have the gethostbyname function.  */
#define HAVE_GETHOSTBYNAME 1

/* Define if you have the gethostbyname_r function.  */
/* #undef HAVE_GETHOSTBYNAME_R */

/* Define if you have the getrlimit function.  */
#define HAVE_GETRLIMIT 1

/* Define if you have the gettimeofday function.  */
#define HAVE_GETTIMEOFDAY 1

/* Define if you have the inet_addr function.  */
#define HAVE_INET_ADDR 1

/* Define if you have the inet_aton function.  */
#define HAVE_INET_ATON 1

/* Define if you have the inet_ntoa function.  */
#define HAVE_INET_NTOA 1

/* Define if you have the localtime_r function.  */
#define HAVE_LOCALTIME_R 1

/* Define if you have the log function.  */
#define HAVE_LOG 1

/* Define if you have the lseek function.  */
#define HAVE_LSEEK 1

/* Define if you have the mallinfo function.  */
/* #undef HAVE_MALLINFO */

/* Define if you have the mcheck function.  */
#undef HAVE_MCHECK

/* Define if you have the mtrace function.  */
/* #undef HAVE_MTRACE */

/* Define if you have the nanosleep function.  */
#define HAVE_NANOSLEEP 1

/* Define if you have the pthread_attr_setstacksize function.  */
#define HAVE_PTHREAD_ATTR_SETSTACKSIZE 1

/* Define if you have the pthread_create function.  */
/* #undef HAVE_PTHREAD_CREATE */

/* Define if you have the pthread_sigmask function.  */
#define HAVE_PTHREAD_SIGMASK 1

/* Define if you have the rename function.  */
#define HAVE_RENAME 1

/* Define if you have the select function.  */
#define HAVE_SELECT 1

/* Define if you have the setpgid function.  */
#define HAVE_SETPGID 1

/* Define if you have the setrlimit function.  */
#define HAVE_SETRLIMIT 1

/* Define if you have the setsockopt function.  */
#define HAVE_SETSOCKOPT 1

/* Define if you have the sigaction function.  */
#define HAVE_SIGACTION 1

/* Define if you have the snprintf function.  */
#define HAVE_SNPRINTF 1

/* Define if you have the socket function.  */
#define HAVE_SOCKET 1

/* Define if you have the strstr function.  */
#define HAVE_STRSTR 1

/* Define if you have the umask function.  */
#define HAVE_UMASK 1

/* Define if you have the vsnprintf function.  */
#define HAVE_VSNPRINTF 1

/* Define if you have the yp_get_default_domain function.  */
/* #undef HAVE_YP_GET_DEFAULT_DOMAIN */

/* Define if you have the <Python.h> header file.  */
/* #undef HAVE_PYTHON_H */

/* Define if you have the <assert.h> header file.  */
#define HAVE_ASSERT_H 1

/* Define if you have the <dirent.h> header file.  */
#define HAVE_DIRENT_H 1

/* Define if you have the <fcntl.h> header file.  */
#define HAVE_FCNTL_H 1

/* Define if you have the <history.h> header file.  */
/* #undef HAVE_HISTORY_H */

/* Define if you have the <machine/soundcard.h> header file.  */
/* #undef HAVE_MACHINE_SOUNDCARD_H */

/* Define if you have the <malloc.h> header file.  */
/* #undef HAVE_MALLOC_H */

/* Define if you have the <math.h> header file.  */
#define HAVE_MATH_H 1

/* Define if you have the <mcheck.h> header file.  */
/* #undef HAVE_MCHECK_H */

/* Define if you have the <ndir.h> header file.  */
/* #undef HAVE_NDIR_H */

/* Define if you have the <pthread.h> header file.  */
#define HAVE_PTHREAD_H 1

/* Define if you have the <signal.h> header file.  */
#define HAVE_SIGNAL_H 1

/* Define if you have the <sys/dir.h> header file.  */
/* #undef HAVE_SYS_DIR_H */

/* Define if you have the <sys/ndir.h> header file.  */
/* #undef HAVE_SYS_NDIR_H */

/* Define if you have the <sys/resource.h> header file.  */
#define HAVE_SYS_RESOURCE_H 1

/* Define if you have the <sys/signal.h> header file.  */
#define HAVE_SYS_SIGNAL_H 1

/* Define if you have the <sys/soundcard.h> header file.  */
/* #undef HAVE_SYS_SOUNDCARD_H */

/* Define if you have the <sys/time.h> header file.  */
#define HAVE_SYS_TIME_H 1

/* Define if you have the <unistd.h> header file.  */
#define HAVE_UNISTD_H 1

/* Define if you have the dl library (-ldl).  */
/* #undef HAVE_LIBDL */

/* Define if you have the readline library (-lreadline).  */
/* #undef HAVE_LIBREADLINE */

/* Name of package */
#define PACKAGE "ntripcaster"

/* Version number of package */
#define VERSION "0.1.5"

