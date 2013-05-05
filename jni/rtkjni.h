
#ifndef _RTKJNI_H
#define _RTKJNI_H

#include "rtklib.h"

int registerRtkServerNatives(JNIEnv* env);
int registerRtkCommonNatives(JNIEnv* env);
int registerGTimeNatives(JNIEnv* env);
int registerSolutionOptionsNatives(JNIEnv* env);
int registerProcessingOptionsNatives(JNIEnv* env);

void processing_options2prcopt_t(JNIEnv* env, jobject thiz, prcopt_t *dst);
void solution_options2solopt_t(JNIEnv* env, jobject thiz, solopt_t *dst);

/* rtkjni.h */
void j_str2buf(JNIEnv* env, jstring str, char *dest, size_t n);

/* gtime.c */
void set_gtime(JNIEnv* env, jclass jgtime, gtime_t time);


#endif /* _RTKJNI_H  */
