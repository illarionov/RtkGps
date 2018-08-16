/* timer.c
 * - Thread for periodic events
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
#include <errno.h>

#ifndef __USE_BSD
#  define __USE_BSD
#endif

#ifndef __EXTENSIONS__
# define __EXTENSIONS__
#endif

#include <string.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#include <fcntl.h>

#ifndef _WIN32
#include <sys/socket.h>
#include <netinet/in.h>
#endif

#include <sys/types.h>
#include <time.h>
#include <stdlib.h>

#include "avl.h"
#include "threads.h"
#include "ntripcaster.h"
#include "utility.h"
#include "ntrip_string.h"
#include "threads.h"
#include "timer.h"
#include "log.h"
#include "sock.h"
#include "client.h"
#include "source.h"

#ifndef MSG_DONTWAIT
#define MSG_DONTWAIT 0
#endif

/* extern int errno; */
extern int running;
extern server_info_t info;

void display_stats(statistics_t *stat);

/* Writes the one line status report to the log and the console if needed */
void status_write(server_info_t *infostruct)
{
	char *lt = get_log_time();

//	if (running == SERVER_RUNNING) info.num_clients = (unsigned long int) count_clients();

	android_log(ANDROID_LOG_VERBOSE, "Bandwidth:%fKB/s Sources:%ld Clients:%ld", info.bandwidth_usage, info.num_sources, info.num_clients);

	if (lt)
		free(lt);

}

/* Starts up the calendar thread.
 */
void *startup_timer_thread(void *arg)
{
	time_t justone = 0, trottime = 0;
	statistics_t trotstat;
	mythread_t *mt;

	thread_init();

	mt = thread_get_mythread();

	while (thread_alive (mt)) {
		time_t stime = get_time();

		timer_handle_status_lines (stime);

		timer_handle_transfer_statistics (stime, &trottime, &justone, &trotstat);

		if (mt->ping == 1)
			mt->ping = 0;

		my_sleep(400000);
	}
	
	thread_exit(7);
	return NULL;
}

void
timer_handle_status_lines (time_t stime)
{
	if ((stime - info.statuslasttime) >= info.statustime) {
		info.statuslasttime = stime;
		status_write(&info);
	}
	
}


void
timer_handle_transfer_statistics (time_t stime, time_t *trottime, time_t *justone, statistics_t *trotstat)
{
	if (get_time() != *justone) {
		*justone = get_time();
		
		if ((stime % 86400) == 0) {
			statistics_t stat, hourlystats;
			
			get_hourly_stats(&hourlystats);
			zero_stats(&info.hourly_stats);
			update_daily_statistics(&hourlystats);
			
			get_daily_stats(&stat);
			zero_stats(&info.daily_stats);
			update_total_statistics(&stat);
			write_daily_stats(&stat);
		} else if ((stime % 3600) == 0) {
			statistics_t stat;
			get_hourly_stats(&stat);
			zero_stats(&info.hourly_stats);
			update_daily_statistics(&stat);
			write_hourly_stats(&stat);
		}
		
		if ((stime % 60) == 0) {
			time_t delta;
			statistics_t stat;
			unsigned int total_bytes;

			double KB_per_sec = 0;
			
			get_running_stats(&stat);
			
			if (*trottime == 0) {
				*trottime = get_time();
				get_running_stats(trotstat);
			} else {
				total_bytes = (stat.read_kilos - trotstat->read_kilos) + (stat.write_kilos - trotstat->write_kilos);
				delta = get_time() - *trottime;
				if (delta <= 0) {
					android_log(ANDROID_LOG_VERBOSE,
						"ERROR: Losing track of time.. is it xmas already? [%d - %d == %d <= 0]", 
						get_time (), *trottime, delta);
				} else {
					KB_per_sec = (double)total_bytes / (double)delta;

					if (KB_per_sec < 40000000) {
						info.bandwidth_usage = KB_per_sec;

					}
				}
				
				get_running_stats(trotstat);
				*trottime = get_time();
			}
		}
	}
}

void get_hourly_stats(statistics_t *stat)
{
	stat->read_bytes = info.hourly_stats.read_bytes;
	stat->write_bytes = info.hourly_stats.write_bytes;
	stat->client_connections = info.hourly_stats.client_connections;
	stat->source_connections = info.hourly_stats.source_connections;
	stat->client_connect_time = info.hourly_stats.client_connect_time;
	stat->source_connect_time = info.hourly_stats.source_connect_time;
}

void write_hourly_stats(statistics_t *stat)
{
	char cct[BUFSIZE], sct[BUFSIZE];
	char timebuf[BUFSIZE];
	statistics_t running;

	get_current_stats(&running);
	add_stats(stat, &running, 0);

	strncpy(cct, connect_average (stat->client_connect_time, stat->client_connections + info.num_clients, timebuf), BUFSIZE);
	strncpy(sct, connect_average (stat->source_connect_time, stat->source_connections + info.num_sources, timebuf), BUFSIZE);
		 
}

void update_daily_statistics(statistics_t *stat)
{
	thread_mutex_lock(&info.misc_mutex);
	info.daily_stats.read_bytes += (stat->read_bytes / 1000);
	info.daily_stats.write_bytes += (stat->write_bytes / 1000);
	info.daily_stats.client_connections += stat->client_connections;
	info.daily_stats.source_connections += stat->source_connections;
	info.daily_stats.client_connect_time += stat->client_connect_time;
	info.daily_stats.source_connect_time += stat->source_connect_time;
	thread_mutex_unlock(&info.misc_mutex);
}

void get_daily_stats (statistics_t *stat)
{
	thread_mutex_lock(&info.misc_mutex);
	stat->read_bytes = info.daily_stats.read_bytes;
	stat->write_bytes = info.daily_stats.write_bytes;
	stat->client_connections = info.daily_stats.client_connections;
	stat->source_connections = info.daily_stats.source_connections;
	stat->client_connect_time = info.daily_stats.client_connect_time;
	stat->source_connect_time = info.daily_stats.source_connect_time;
	thread_mutex_unlock(&info.misc_mutex);
}

void update_total_statistics(statistics_t *stat)
{
	thread_mutex_lock(&info.misc_mutex);
	info.total_stats.read_bytes += (stat->read_bytes / 1000);
	info.total_stats.read_kilos += (stat->read_bytes);

	info.total_stats.write_bytes += (stat->write_bytes / 1000);
	info.total_stats.write_kilos += (stat->write_bytes);

	info.total_stats.client_connections += stat->client_connections;
	info.total_stats.source_connections += stat->source_connections;
	info.total_stats.client_connect_time += stat->client_connect_time;
	info.total_stats.source_connect_time += stat->source_connect_time;
	thread_mutex_unlock(&info.misc_mutex);
}

void write_daily_stats(statistics_t *stat)
{
	char cct[BUFSIZE], sct[BUFSIZE];
	statistics_t running;
	char timebuf[BUFSIZE];

	get_current_stats(&running);
	add_stats(stat, &running, 0);

	strncpy(cct, connect_average (stat->client_connect_time, stat->client_connections + info.num_clients, timebuf), BUFSIZE);
	strncpy(sct, connect_average (stat->source_connect_time, stat->source_connections + info.num_sources, timebuf), BUFSIZE);
}
		
void get_current_stats(statistics_t *stat)
{
	get_current_stats_proc (stat, 1);
}

void get_current_stats_proc (statistics_t *stat, int lock)
{
	avl_traverser trav = {0};
	time_t ec = 0, cc = 0;
	connection_t *travcon;

	zero_stats(stat);
	
	if (lock)
		thread_mutex_lock(&info.double_mutex);
	thread_mutex_lock(&info.source_mutex);

	ec = (time_t)tree_time(info.sources);
	while ((travcon = avl_traverse(info.sources, &trav))) {
		thread_mutex_lock(&travcon->food.source->mutex);
		cc += (time_t)tree_time(travcon->food.source->clients);
		thread_mutex_unlock(&travcon->food.source->mutex);
	}

	thread_mutex_unlock(&info.source_mutex);
	
	if (lock)
		thread_mutex_unlock(&info.double_mutex);

	stat->client_connect_time = cc;
	stat->source_connect_time = ec;
}

void get_running_stats(statistics_t *stat)
{
	get_running_stats_proc (stat, 1);
}

void get_running_stats_proc (statistics_t *stat, int lock)
{
	statistics_t bufstat;

// megabytes
	stat->read_bytes = info.total_stats.read_bytes;
	stat->write_bytes = info.total_stats.write_bytes;

// kilobytes
	stat->read_kilos = info.total_stats.read_kilos;
	stat->write_kilos = info.total_stats.write_kilos;

	stat->client_connections = info.total_stats.client_connections;
	stat->source_connections = info.total_stats.source_connections;
	stat->client_connect_time = info.total_stats.client_connect_time;
	stat->source_connect_time = info.total_stats.source_connect_time;
	
// bytes
	get_current_stats_proc (&bufstat, lock);
	add_stats(stat, &bufstat, 0);

// bytes
	get_hourly_stats(&bufstat);
	add_stats(stat, &bufstat, 0);
	
// kilobytes
	get_daily_stats(&bufstat);
	add_stats(stat, &bufstat, 1000);
}


void zero_stats(statistics_t *stat)
{
	if (!stat) {
		android_log (ANDROID_LOG_VERBOSE, "WARNING: zero_stats() called with NULL stat pointer");
		return;
	}

	stat->read_bytes = 0;
	stat->read_kilos = 0;

	stat->write_bytes = 0;
	stat->write_kilos = 0;

	stat->client_connections = 0;
	stat->source_connections = 0;
	stat->client_connect_time = 0;
	stat->source_connect_time = 0;
}

void add_stats(statistics_t *target, statistics_t *source, unsigned long int factor)
{
	double div;

	if (factor == 0)
		div = 1000000.0;
	else 
		div = (1000000.0 / (double)factor);
	
	target->read_bytes += (unsigned long)(source->read_bytes / div);
	target->read_kilos += (unsigned long)(source->read_bytes / (div / 1000));

	target->write_bytes += (unsigned long)(source->write_bytes / div);
	target->write_kilos += (unsigned long)(source->write_bytes / (div / 1000));

	target->client_connections += source->client_connections;
	target->client_connect_time += source->client_connect_time;
	target->source_connections += source->source_connections;
	target->source_connect_time += source->source_connect_time;
}
