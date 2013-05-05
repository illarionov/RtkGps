
#include <android/log.h>
#include <jni.h>
#include <strings.h>

#include "rtkjni.h"

#define TAG "nativeRtk"
#define LOGV(...) showmsg(__VA_ARGS__)

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    (void)reserved;

    LOGV("Entering JNI_OnLoad");

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK)
        goto bail;

    if (!registerRtkServerNatives(env))
        goto bail;

    if (!registerRtkCommonNatives(env))
        goto bail;

    if (!registerGTimeNatives(env))
        goto bail;

    if (!registerSolutionOptionsNatives(env))
        goto bail;

    if (!registerProcessingOptionsNatives(env))
        goto bail;

    /* success -- return valid version number */
    result = JNI_VERSION_1_6;

bail:
    LOGV("Leaving JNI_OnLoad (result=0x%x)", result);
    return result;
}


void j_str2buf(JNIEnv* env, jstring str, char *dest, size_t n)
{
   int string_size;

   if (n == 0 || dest == NULL)
      return;

   if (str == NULL) {
      dest[0] = '\0';
      return;
   }

   string_size = (*env)->GetStringUTFLength(env, str);
   if (string_size > n-1) {
      LOGV("String too long");
      dest[0] = '\0';
   }else if (string_size == 0) {
      dest[0] = '\0';
   }else {
      (*env)->GetStringUTFRegion(env,
	    str,
	    0,
	    (*env)->GetStringLength(env, str),
	    dest);
      dest[string_size] = '\0';
   }
}


