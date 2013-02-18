
#include <android/log.h>
#include <jni.h>
#include <strings.h>

#include "rtklib.h"

#define TAG "nativeRtkServer"
#define LOGV(...) showmsg(__VA_ARGS__)


static jfieldID m_object_field;

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

static jboolean RtkServer_start(JNIEnv* env, jclass thiz)
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

   LOGV("RtkServer_start()");

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

static void RtkServer_stop(JNIEnv* env, jclass thiz)
{
   struct native_ctx_t *nctx;
   char *cmds[3]={0,};

   LOGV("RtkServer_stop()");

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

static JNINativeMethod nativeMethods[] = {
   {"_create", "()V", (void*)RtkServer__create},
   {"_destroy", "()V", (void*)RtkServer__destroy},
   {"start", "()Z", (void*)RtkServer_start},
   {"stop", "()V", (void*)RtkServer_stop},
   {"_getStreamStatus", "(Lru0xdc/rtklib/RtkServerStreamStatus;)V", (void*)RtkServer__get_stream_status}
};

static int registerNatives(JNIEnv* env) {
    int result = -1;

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

    return JNI_TRUE;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    LOGV("Entering JNI_OnLoad");

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK)
        goto bail;

    if (!registerNatives(env))
        goto bail;

    /* success -- return valid version number */
    result = JNI_VERSION_1_6;

bail:
    LOGV("Leaving JNI_OnLoad (result=0x%x)", result);
    return result;
}

