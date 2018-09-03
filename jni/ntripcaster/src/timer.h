/* timer.h
 * - timer function headers
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

#ifndef ICECAST_TIMER_H
#define ICECAST_TIMER_H

void *startup_timer_thread(void *arg);
void status_write(server_info_t *info);
void get_hourly_stats(statistics_t *stat);
void write_hourly_stats(statistics_t *stat);
void update_daily_statistics(statistics_t *stat);
void get_daily_stats(statistics_t *stat);
void update_total_statistics(statistics_t *stat);
void write_daily_stats(statistics_t *stat);
void zero_stats(statistics_t *stat);
void get_current_stats(statistics_t *stat);
void get_current_stats_proc (statistics_t *stat, int lock);
void get_running_stats(statistics_t *stat);
void get_running_stats_proc (statistics_t *stat, int lock);
void add_stats(statistics_t *target, statistics_t *source, unsigned long int factor);
void timer_handle_status_lines (time_t stime);
void timer_handle_transfer_statistics (time_t stime, time_t *trottime, time_t *justone, statistics_t *trotstat);
#endif

