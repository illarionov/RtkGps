/* main.h
 * - Main Function Headers
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

#ifndef __ICECAST_MAIN_H
#define __ICECAST_MAIN_H

void *threaded_server_proc(void *infoarg);
void client_login(connection_t *con, char *line);
void setup_defaults();
void setup_signal_traps();
void allocate_resources();
void startup_mode();
void clean_shutdown(server_info_t *info);
void usage();
void setup_listeners();
void initialize_network ();
#ifdef _WIN32
BOOL WINAPI win_sig_die (DWORD CtrlType);
#else
RETSIGTYPE sig_hup(int signo);
RETSIGTYPE sig_die(int signo);
RETSIGTYPE sig_die_hard (int signo);
RETSIGTYPE sig_child(int signo);
#endif
char *splitc(char *first, char *rest, const char divider);
void close_socket(sock_t sock);
int get_ntripcaster_state();

#endif
