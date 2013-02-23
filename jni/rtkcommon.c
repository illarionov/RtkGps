
#include <android/log.h>
#include <jni.h>
#include <strings.h>

#include "rtklib.h"

#define TAG "nativeRtkCommion"
#define LOGV(...) showmsg(__VA_ARGS__)

static jobject RtkCommon_get_sat_id(JNIEnv* env, jclass clazz, jint sat_no)
{
   char sat_id[16];
   satno2id(sat_no, sat_id);
   return (*env)->NewStringUTF(env, sat_id);
}

static void RtkCommon_dops(JNIEnv* env, jclass clazz, jdoubleArray j_azel, jint j_ns, jdouble j_elmin, jobject j_dst)
{
   double dst[4];
   double *azel;
   jmethodID set_dops_mid;

   azel = (*env)->GetDoubleArrayElements(env, j_azel, NULL);
   if (azel == NULL)
      return;

   dops(j_ns, azel, j_elmin, dst);

   (*env)->ReleaseDoubleArrayElements(env, j_azel, azel, 0);

   set_dops_mid = (*env)->GetMethodID(env,
	 (*env)->GetObjectClass(env, j_dst),
	 "setDops",
	 "(DDDD)V");
   if (set_dops_mid == NULL) {
      LOGV("setDops() not found");
      return;
   }

   (*env)->CallVoidMethod(env, j_dst, set_dops_mid,
	 dst[0], dst[1], dst[2], dst[3]);
}

static JNINativeMethod nativeMethods[] = {
   {"getSatId", "(I)Ljava/lang/String;", (void*)RtkCommon_get_sat_id},
   {"dops", "([DIDLru0xdc/rtklib/Dops;)V", (void*)RtkCommon_dops}
};

int registerRtkCommonNatives(JNIEnv* env) {
    int result = -1;

    /* look up the class */
    jclass clazz = (*env)->FindClass(env, "ru0xdc/rtklib/RtkCommon");

    if (clazz == NULL)
       return JNI_FALSE;

    if ((*env)->RegisterNatives(env, clazz, nativeMethods, sizeof(nativeMethods)
	     / sizeof(nativeMethods[0])) != JNI_OK)
       return JNI_FALSE;

    return JNI_TRUE;
}

