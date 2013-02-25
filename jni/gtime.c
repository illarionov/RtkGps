
#include <android/log.h>
#include <jni.h>
#include <strings.h>

#include "rtklib.h"

#define TAG "nativeGTime"
#define LOGV(...) showmsg(__VA_ARGS__)

static struct {
   jfieldID time;
   jfieldID sec;
} ru0xdc_rtklib_gtime_fields;


static gtime_t get_gtime_t(JNIEnv* env, jobject thiz)
{
   gtime_t gt;
   gt.time = (*env)->GetLongField(env, thiz, ru0xdc_rtklib_gtime_fields.time);
   gt.sec = (*env)->GetDoubleField(env, thiz, ru0xdc_rtklib_gtime_fields.sec);
   return gt;
}

static void GTime_get_android_utc_time(JNIEnv* env, jobject thiz, jobject j_dst)
{
   static jmethodID set_time_mid = NULL;
   gtime_t t;

   t = get_gtime_t(env, thiz);
   t = gpst2utc(t);

   if (set_time_mid == NULL) {
      set_time_mid = (*env)->GetMethodID(env,
	    (*env)->GetObjectClass(env, j_dst),
	    "set",
	    "(J)V");
      if (set_time_mid == NULL) {
	 LOGV("set(long mills) not found");
	 return;
      }
   }
   (*env)->CallVoidMethod(env, j_dst, set_time_mid, (jlong)(1000ll * t.time + llround(1000.0 * t.sec)));
}

static jint GTime_get_gps_week(JNIEnv* env, jobject thiz)
{
   int week;
   time2gpst(get_gtime_t(env, thiz), &week);
   return week;
}

static jdouble GTime_get_gps_tow(JNIEnv* env, jobject thiz)
{
   return time2gpst(get_gtime_t(env, thiz), NULL);
}

static int init_gtime_fields(JNIEnv* env, jclass clazz) {

    ru0xdc_rtklib_gtime_fields.time = (*env)->GetFieldID(env, clazz, "time", "J");
    ru0xdc_rtklib_gtime_fields.sec = (*env)->GetFieldID(env, clazz, "sec", "D");

    if ((ru0xdc_rtklib_gtime_fields.time == NULL)
	  || (ru0xdc_rtklib_gtime_fields.sec == NULL)) {
       LOGV("ru0xdc/rtklib/GTime fields not found:%s%s",
	     (ru0xdc_rtklib_gtime_fields.time == NULL ? " time" : ""),
	     (ru0xdc_rtklib_gtime_fields.sec == NULL ? " sec" : ""));
       return JNI_FALSE;
    }

    return JNI_TRUE;
}


static JNINativeMethod nativeMethods[] = {
   { "_getAndroidUtcTime", "(Landroid/text/format/Time;)V", (void*)GTime_get_android_utc_time },
   { "getGpsWeek", "()I", (void*)GTime_get_gps_week },
   { "getGpsTow", "()D", (void*)GTime_get_gps_tow }
};

int registerGTimeNatives(JNIEnv* env) {
    int result = -1;

    /* look up the class */
    jclass clazz = (*env)->FindClass(env, "ru0xdc/rtklib/GTime");

    if (clazz == NULL)
       return JNI_FALSE;

    if ((*env)->RegisterNatives(env, clazz, nativeMethods, sizeof(nativeMethods)
	     / sizeof(nativeMethods[0])) != JNI_OK)
       return JNI_FALSE;

    if ( ! init_gtime_fields(env, clazz))
       return JNI_FALSE;

    return JNI_TRUE;
}

