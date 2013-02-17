
#include <android/log.h>
#include <jni.h>
#include <stdbool.h>
#include <strings.h>

#include "rtklib.h"

#define TAG "nativeRtkNaviService"
#define LOGV(...) showmsg(__VA_ARGS__)

#define RTK_PUBLIC __attribute__ ((visibility ("default")))

static rtksvr_t rtksvr;                        // rtk server struct
static stream_t monistr;                       // monitor stream


RTK_PUBLIC void Java_ru0xdc_rtkgps_RtkNaviService_rtkSrvInit(JNIEnv* env, jclass clazz)
{
   LOGV("rtkSrvInit() %u %u", sizeof(rtksvr_t), sizeof(stream_t));

   /*
   rtksvr = (rtksvr_t *)calloc(1, sizeof(rtksvr_t));
   if (rtksvr == NULL) {
      LOGV("malloc(rtksvr_t %u) error",  sizeof(rtksvr));
      return;
   }

   monistr = (stream_t *)calloc(1, sizeof(stream_t));
   if (monistr == NULL) {
      LOGV("malloc(monistr %u) error",  sizeof(stream_t));
      free(rtksvr);
      return;
   }
*/
   rtksvrinit(&rtksvr);
   strinit(&monistr);
}

RTK_PUBLIC jboolean Java_ru0xdc_rtkgps_RtkNaviService_rtkSrvStart(JNIEnv* env, jclass clazz)
{
   /* Input stream types */
   int strs[MAXSTRRTK] = {
      STR_NTRIPCLI, STR_NONE, STR_NONE,
      STR_NONE, STR_NONE,
      STR_NONE, STR_NONE, STR_NONE,
   };

   /* input stream paths */
   char *paths[8] = {"osm:osm@gps.0xdc.ru:2101/gag27-rtcm:", "", "",
      "", "", "", "", ""};

   /* input stream formats (STRFMT_???) */
   int format[3] = {STRFMT_RTCM3, STRFMT_RTCM3, STRFMT_RTCM3};

   /* input stream start commands */
   char *cmds[3] = {NULL, NULL, NULL};

   /* receiver options */
   char *rcvopts[3] = {"", "", ""};

   /* rtk processing options */
   prcopt_t prcopt;

   /* solution options */
   solopt_t solopt[3];

   /* nmea position */
   double nmeapos[3] = {0.0, };

   prcopt = prcopt_default;
   prcopt.mode = PMODE_PPP_STATIC;

   bzero(&solopt[0], sizeof(solopt_t));
   bzero(&solopt[1], sizeof(solopt_t));
   solopt[1] = solopt_default;
   solopt[2] = solopt_default;

   LOGV("rtkSrvStart()");

   if (!rtksvrstart(
	    &rtksvr,
	    /* SvrCycle ms */ 10,
	    /* SvrBuffSize */ 32768,
	    /* stream types */ strs,
	    /* paths */ paths,
	    /* input stream format */ format,
	    /* NavSelect */ 0,
	    /* input stream start commands */ cmds,
	    /* receiver options */ rcvopts,
	    /* nmea request cycle ms */ 1000,
	    /* nmea request type */ 1,
	    /* transmitted nmea position  */ nmeapos,
	    /* rtk processing options */&prcopt,
	    /* solution options */ solopt,
	    /* monitor stream */ &monistr
	    )) {
      traceclose();
      return false;
   }

   return true;
}

RTK_PUBLIC void Java_ru0xdc_rtkgps_RtkNaviService_rtkSrvStop(JNIEnv* env, jclass clazz)
{
   char *cmds[3]={0,};

   LOGV("rtkSrvStop()");

   rtksvrstop(&rtksvr,cmds);

}

