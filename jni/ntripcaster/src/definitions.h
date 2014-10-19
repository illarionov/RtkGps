/* definitions.h
 * - Special defines needed by some systems
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

#ifndef __USE_MISC
# define __USE_MISC
#endif

#ifndef __USE_GNU
# define __USE_GNU
#endif

#ifndef __USE_BSD
# define __USE_BSD
#endif

#ifndef __EXTENSIONS__
# define __EXTENSIONS__
#endif

#ifndef _GNU_SOURCE
# define _GNU_SOURCE
#endif

#ifndef _THREAD_SAFE
# define _THREAD_SAFE
#endif

#ifndef _REENTRANT
# define _REENTRANT
#endif

#ifndef __USE_POSIX
# define __USE_POSIX
#endif

#ifndef _POSIX_C_SOURCE
#define _POSIX_C_SOURCE 199506L
#endif

/* This for freebsd (needed on 3.2 at least) */
#ifdef SOMEBSD
# ifndef _POSIX_VERSION
# define _POSIX_VERSION 199309L
# endif
#endif






