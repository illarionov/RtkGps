
#include <android/log.h>
#include <jni.h>
#include <strings.h>

#include "rtklib.h"
#include "rtkjni.h"

#define TAG "nativeSolutionOptions"
#define LOGV(...) showmsg(__VA_ARGS__)

#define SOLUTION_OPTIONS_CLASS "gpsplus/rtklib/SolutionOptions$Native"


static struct {
   jfieldID posf;
   jfieldID times;
   jfieldID timef;
   jfieldID timeu;
   jfieldID degf;
   jfieldID outhead;
   jfieldID outopt;
   jfieldID datum;
   jfieldID height;
   jfieldID geoid;
   jfieldID solstatic;
   jfieldID sstat;
   jfieldID trace;
   jfieldID nmeaintv_rmcgga;
   jfieldID nmeaintv_gsv;
   jfieldID sep;
   jfieldID prog;
} gpsplus_rtklib_solopt_fields;

static int init_solopt_fields_methods(JNIEnv* env, jclass clazz);

void solution_options2solopt_t(JNIEnv* env, jobject thiz, solopt_t *dst)
{
   int string_size;
   jstring str;
   jclass clazz;

   clazz = (*env)->FindClass(env, SOLUTION_OPTIONS_CLASS);
   if (clazz == NULL)
      return;
   if (!(*env)->IsInstanceOf(env, thiz, clazz)) {
      (*env)->ThrowNew(env, (*env)->FindClass(env, "Ljava/lang/ClassCastException"), NULL);
      return;
   }

#define GET_FIELD(_name, _type) { dst->_name = (*env)->Get ## _type ## Field(env, thiz, gpsplus_rtklib_solopt_fields._name); }
   GET_FIELD(posf, Int)
   GET_FIELD(times, Int)
   GET_FIELD(timef, Int)
   GET_FIELD(timeu, Int)
   GET_FIELD(degf, Int)
   GET_FIELD(outhead, Boolean)
   GET_FIELD(outopt, Boolean)
   GET_FIELD(datum, Int)
   GET_FIELD(height, Int)
   GET_FIELD(geoid, Int)
   GET_FIELD(solstatic, Int)
   GET_FIELD(sstat, Int)
   GET_FIELD(trace, Int)
#undef GET_FIELD

   dst->nmeaintv[0] = (*env)->GetDoubleField(env, thiz, gpsplus_rtklib_solopt_fields.nmeaintv_rmcgga);
   dst->nmeaintv[1] = (*env)->GetDoubleField(env, thiz, gpsplus_rtklib_solopt_fields.nmeaintv_gsv);

   str = (*env)->GetObjectField(env, thiz, gpsplus_rtklib_solopt_fields.sep);
   j_str2buf(env, str, dst->sep, sizeof(dst->sep));
   str = (*env)->GetObjectField(env, thiz, gpsplus_rtklib_solopt_fields.prog);
   j_str2buf(env, str, dst->prog, sizeof(dst->prog));
}

static void SolutionOptions_load_defaults(JNIEnv* env, jobject thiz)
{
   jstring jstr;

   if (gpsplus_rtklib_solopt_fields.posf == NULL) {
      if ( ! init_solopt_fields_methods(env, (*env)->GetObjectClass(env, thiz)))
	 return;
   }

#define SET_FIELD(_name, _type) { (*env)->Set ## _type ## Field(env, thiz, gpsplus_rtklib_solopt_fields._name, solopt_default._name); }
   SET_FIELD(posf, Int)
   SET_FIELD(times, Int)
   SET_FIELD(timef, Int)
   SET_FIELD(timeu, Int)
   SET_FIELD(degf, Int)
   SET_FIELD(outhead, Boolean)
   SET_FIELD(outopt, Boolean)
   SET_FIELD(datum, Int)
   SET_FIELD(height, Int)
   SET_FIELD(geoid, Int)
   SET_FIELD(solstatic, Int)
   SET_FIELD(sstat, Int)
   SET_FIELD(trace, Int)
#undef SET_FIELD

   (*env)->SetDoubleField(env, thiz, gpsplus_rtklib_solopt_fields.nmeaintv_rmcgga, solopt_default.nmeaintv[0]);
   (*env)->SetDoubleField(env, thiz, gpsplus_rtklib_solopt_fields.nmeaintv_gsv, solopt_default.nmeaintv[1]);
   jstr = (*env)->NewStringUTF(env, solopt_default.sep);
   if (jstr == NULL)
      return;
   (*env)->SetObjectField(env, thiz, gpsplus_rtklib_solopt_fields.sep, jstr);
   jstr = (*env)->NewStringUTF(env, solopt_default.prog);
   if (jstr == NULL)
      return;
   (*env)->SetObjectField(env, thiz, gpsplus_rtklib_solopt_fields.prog, jstr);
}

static int init_solopt_fields_methods(JNIEnv* env, jclass clazz) {

#define INIT_FIELD(_name, _type) { \
      gpsplus_rtklib_solopt_fields._name = (*env)->GetFieldID(env, clazz, #_name, _type); \
      if (gpsplus_rtklib_solopt_fields._name == NULL) { \
	 LOGV("gpsplus/rtklib/SolutionOptions$Native$%s not found", #_name); \
	 return JNI_FALSE; \
      } \
      }

   INIT_FIELD(posf, "I")
   INIT_FIELD(times, "I")
   INIT_FIELD(timef, "I")
   INIT_FIELD(timeu, "I")
   INIT_FIELD(degf, "I")
   INIT_FIELD(outhead, "Z")
   INIT_FIELD(outopt, "Z")
   INIT_FIELD(datum, "I")
   INIT_FIELD(height, "I")
   INIT_FIELD(geoid, "I")
   INIT_FIELD(solstatic, "I")
   INIT_FIELD(sstat, "I")
   INIT_FIELD(trace, "I")
   INIT_FIELD(nmeaintv_rmcgga, "D")
   INIT_FIELD(nmeaintv_gsv, "D")
   INIT_FIELD(sep, "Ljava/lang/String;")
   INIT_FIELD(prog, "Ljava/lang/String;")

#undef INIT_FIELD

   return JNI_TRUE;
}

static JNINativeMethod nativeMethods[] = {
   { "_loadDefaults", "()V", (void*)SolutionOptions_load_defaults }
};


int registerSolutionOptionsNatives(JNIEnv* env) {
    int result = -1;

    /* look up the class */
    jclass clazz = (*env)->FindClass(env, SOLUTION_OPTIONS_CLASS);

    if (clazz == NULL)
       return JNI_FALSE;

    if ((*env)->RegisterNatives(env, clazz, nativeMethods, sizeof(nativeMethods)
	     / sizeof(nativeMethods[0])) != JNI_OK)
       return JNI_FALSE;

    if ( ! init_solopt_fields_methods(env, clazz))
       return JNI_FALSE;


    return JNI_TRUE;
}

