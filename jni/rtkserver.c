
#include <android/log.h>
#include <jni.h>
#include <strings.h>

#include "rtklib.h"

#define TAG "nativeRtkServer"
#define LOGV(...) showmsg(__VA_ARGS__)


extern int registerRtkCommonNatives(JNIEnv* env);

static jfieldID m_object_field;

static struct {
   jfieldID ns;
   jfieldID time;
   jfieldID sat;
   jfieldID az;
   jfieldID el;
   jfieldID freq1_snr;
   jfieldID freq2_snr;
   jfieldID freq3_snr;
   jfieldID vsat;
} obs_status_fields;

struct native_ctx_t {
   rtksvr_t rtksvr;                        // rtk server struct
   stream_t monistr;                       // monitor stream
};

static void RtkServer__create(JNIEnv* env, jobject thiz)
{
   struct native_ctx_t *nctx;

   LOGV("RtkServer__create()");

   nctx = (struct native_ctx_t *)calloc(1, sizeof(struct native_ctx_t));

   if (nctx == NULL) {
      LOGV("calloc() error");
      return;
   }

   rtksvrinit(&nctx->rtksvr);
   strinit(&nctx->monistr);

   (*env)->SetLongField(env, thiz, m_object_field, (long)nctx);
}

static void RtkServer__destroy(JNIEnv* env, jobject thiz)
{
   struct native_ctx_t *nctx;
   char *cmds[3]={0,};

   LOGV("RtkServer__destroy()");

   nctx = (struct native_ctx_t *)(uintptr_t)(*env)->GetLongField(env, thiz, m_object_field);
   if (nctx == NULL) {
      LOGV("nctx is null");
      return;
   }
   rtksvrstop(&nctx->rtksvr,cmds);

   free(nctx);
   (*env)->SetLongField(env, thiz, m_object_field, 0L);
}

static jboolean RtkServer__start(JNIEnv* env, jclass thiz)
{
   struct native_ctx_t *nctx;

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

   LOGV("RtkServer__start()");

   nctx = (struct native_ctx_t *)(uintptr_t)(*env)->GetLongField(env, thiz, m_object_field);
   if (nctx == NULL) {
      LOGV("nctx is null");
      return JNI_FALSE;
   }

   if (!rtksvrstart(
	    &nctx->rtksvr,
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
	    /* monitor stream */ &nctx->monistr
	    )) {
      traceclose();
      return JNI_FALSE;
   }

   return JNI_TRUE;
}

static void RtkServer__stop(JNIEnv* env, jclass thiz)
{
   struct native_ctx_t *nctx;
   char *cmds[3]={0,};

   LOGV("RtkServer__stop()");

   nctx = (struct native_ctx_t *)(uintptr_t)(*env)->GetLongField(env, thiz, m_object_field);
   if (nctx == NULL) {
      LOGV("nctx is null");
      return;
   }

   rtksvrstop(&nctx->rtksvr,cmds);
}


static void RtkServer__get_stream_status(JNIEnv* env, jclass thiz, jobject status_obj)
{
   struct native_ctx_t *nctx;
   jmethodID set_status_mid;
   jstring jmsg;
   int sstat[MAXSTRRTK];
   char msg[MAXSTRMSG] = {0,};

   nctx = (struct native_ctx_t *)(uintptr_t)(*env)->GetLongField(env, thiz, m_object_field);
   if (nctx == NULL) {
      LOGV("nctx is null");
      return;
   }

   set_status_mid = (*env)->GetMethodID(env,
	 (*env)->GetObjectClass(env, status_obj),
	 "setStatus",
	 "(IIIIIIIILjava/lang/String;)V");
   if (set_status_mid == NULL) {
      LOGV("setStatus() not found");
      return;
   }

   rtksvrsstat(&nctx->rtksvr, sstat, msg);

   jmsg = (*env)->NewStringUTF(env, msg);
   if (jmsg == NULL)
      return;

   (*env)->CallVoidMethod(env, status_obj, set_status_mid,
	 (jint)sstat[0], (jint)sstat[1], (jint)sstat[2],
	 (jint)sstat[3], (jint)sstat[4], (jint)sstat[5],
	 (jint)sstat[6], (jint)sstat[7],
	 jmsg);

}

static void RtkServer__get_observation_status(JNIEnv* env, jclass thiz,
      jint receiver, jobject status_obj)
{
   struct native_ctx_t *nctx;
   jobject jgtime, jtmp;
   jmethodID set_gtime_mid;
   int ns;
   int i;
   jint *jsnr;
   gtime_t time;
   int sat[MAXSAT];
   int vsat[MAXSAT];
   double az[MAXSAT];
   double el[MAXSAT];
   int *snr0[MAXSAT];
   int snr[MAXSAT][NFREQ];

   nctx = (struct native_ctx_t *)(uintptr_t)(*env)->GetLongField(env, thiz, m_object_field);
   if (nctx == NULL) {
      LOGV("nctx is null");
      return;
   }

   for (i=0;i<MAXSAT;i++) {
      snr0[i]=snr[i];
   }

   ns = rtksvrostat(&nctx->rtksvr, receiver,
	 &time, sat, az, el, snr0, vsat);

   if (ns > MAXSAT)
      ns = MAXSAT;

   // Set number of satellites
   (*env)->SetIntField(env, status_obj, obs_status_fields.ns, ns);

   // Set time
   jgtime = (*env)->GetObjectField(env, status_obj, obs_status_fields.time);
   if (jgtime == NULL) {
      LOGV("time is null");
   }
   set_gtime_mid= (*env)->GetMethodID(env,
	 (*env)->GetObjectClass(env, jgtime),
	 "setGTime",
	 "(JD)V");
   if (set_gtime_mid == NULL) {
      LOGV("setGTime() not found");
      return;
   }
   (*env)->CallVoidMethod(env, jgtime, set_gtime_mid,
	 (jlong)time.time,
	 (jdouble)time.sec);

   if (ns == 0)
      return;

   // Satellite data
   jtmp = (*env)->GetObjectField(env, status_obj, obs_status_fields.sat);
   (*env)->SetIntArrayRegion(env, jtmp, 0, ns, sat);

   jtmp = (*env)->GetObjectField(env, status_obj, obs_status_fields.az);
   (*env)->SetDoubleArrayRegion(env, jtmp, 0, ns, az);

   jtmp = (*env)->GetObjectField(env, status_obj, obs_status_fields.el);
   (*env)->SetDoubleArrayRegion(env, jtmp, 0, ns, el);

   // freq1 snr
   jtmp = (*env)->GetObjectField(env, status_obj, obs_status_fields.freq1_snr);
   jsnr = (*env)->GetIntArrayElements(env, jtmp, NULL);
   if (jsnr != NULL) {
      for (i=0; i<ns; ++i) jsnr[i] = snr[i][0];
      (*env)->ReleaseIntArrayElements(env, jtmp, jsnr, 0);
   }

   jtmp = (*env)->GetObjectField(env, status_obj, obs_status_fields.freq2_snr);
   jsnr = (*env)->GetIntArrayElements(env, jtmp, NULL);
   if (jsnr != NULL) {
      for (i=0; i<ns; ++i) jsnr[i] = snr[i][1];
      (*env)->ReleaseIntArrayElements(env, jtmp, jsnr, 0);
   }

   jtmp = (*env)->GetObjectField(env, status_obj, obs_status_fields.freq3_snr);
   jsnr = (*env)->GetIntArrayElements(env, jtmp, NULL);
   if (jsnr != NULL) {
      for (i=0; i<ns; ++i) jsnr[i] = snr[i][2];
      (*env)->ReleaseIntArrayElements(env, jtmp, jsnr, 0);
   }

   // vsat
   jtmp = (*env)->GetObjectField(env, status_obj, obs_status_fields.vsat);
   (*env)->SetIntArrayRegion(env, jtmp, 0, ns, vsat);

}

static JNINativeMethod nativeMethods[] = {
   {"_create", "()V", (void*)RtkServer__create},
   {"_destroy", "()V", (void*)RtkServer__destroy},
   {"_start", "()Z", (void*)RtkServer__start},
   {"_stop", "()V", (void*)RtkServer__stop},
   {"_getStreamStatus", "(Lru0xdc/rtklib/RtkServerStreamStatus;)V", (void*)RtkServer__get_stream_status},
   {"_getObservationStatus", "(ILru0xdc/rtklib/RtkServerObservationStatus;)V", (void*)RtkServer__get_observation_status}
};

static int init_observation_status_fields(JNIEnv* env) {
    jclass clazz = (*env)->FindClass(env, "ru0xdc/rtklib/RtkServerObservationStatus");

    if (clazz == NULL)
       return JNI_FALSE;

    obs_status_fields.ns = (*env)->GetFieldID(env, clazz, "ns", "I");
    obs_status_fields.time = (*env)->GetFieldID(env, clazz, "time", "Lru0xdc/rtklib/GTime;");
    obs_status_fields.sat = (*env)->GetFieldID(env, clazz, "sat", "[I");
    obs_status_fields.az = (*env)->GetFieldID(env, clazz, "az", "[D");
    obs_status_fields.el = (*env)->GetFieldID(env, clazz, "el", "[D");
    obs_status_fields.freq1_snr = (*env)->GetFieldID(env, clazz, "freq1Snr", "[I");
    obs_status_fields.freq2_snr = (*env)->GetFieldID(env, clazz, "freq2Snr", "[I");
    obs_status_fields.freq3_snr = (*env)->GetFieldID(env, clazz, "freq3Snr", "[I");
    obs_status_fields.vsat = (*env)->GetFieldID(env, clazz, "vsat", "[I");

    if ((obs_status_fields.ns == NULL)
	  || (obs_status_fields.time == NULL)
	  || (obs_status_fields.sat == NULL)
	  || (obs_status_fields.az == NULL)
	  || (obs_status_fields.el == NULL)
	  || (obs_status_fields.freq1_snr == NULL)
	  || (obs_status_fields.freq2_snr == NULL)
	  || (obs_status_fields.freq3_snr == NULL)
	  || (obs_status_fields.vsat == NULL)) {
       LOGV("ru0xdc/rtklib/RtkServerObservationStatus fields not found:%s%s%s%s%s%s%s%s%s",
	     (obs_status_fields.ns == NULL ? " ns" : ""),
	     (obs_status_fields.time == NULL ? " time" : ""),
	     (obs_status_fields.sat == NULL ? " sat" : ""),
	     (obs_status_fields.az == NULL ? " az" : ""),
	     (obs_status_fields.el == NULL ? " el" : ""),
	     (obs_status_fields.freq1_snr == NULL ? " freq1_snr " : ""),
	     (obs_status_fields.freq2_snr == NULL ? " freq2_snr " : ""),
	     (obs_status_fields.freq3_snr == NULL ? " freq3_snr " : ""),
	     (obs_status_fields.vsat == NULL ? " vsat" : "")
	     );
       return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int registerNatives(JNIEnv* env) {
    /* look up the class */
    jclass clazz = (*env)->FindClass(env, "ru0xdc/rtklib/RtkServer");

    if (clazz == NULL)
       return JNI_FALSE;

    if ((*env)->RegisterNatives(env, clazz, nativeMethods, sizeof(nativeMethods)
	     / sizeof(nativeMethods[0])) != JNI_OK)
       return JNI_FALSE;

    m_object_field = (*env)->GetFieldID(env, clazz, "mObject", "J");
    if (m_object_field == NULL)
       return JNI_FALSE;

    if ( ! init_observation_status_fields(env))
       return JNI_FALSE;

    return JNI_TRUE;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    (void)reserved;

    LOGV("Entering JNI_OnLoad");

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK)
        goto bail;

    if (!registerNatives(env))
        goto bail;

    if (!registerRtkCommonNatives(env))
        goto bail;

    /* success -- return valid version number */
    result = JNI_VERSION_1_6;

bail:
    LOGV("Leaving JNI_OnLoad (result=0x%x)", result);
    return result;
}

