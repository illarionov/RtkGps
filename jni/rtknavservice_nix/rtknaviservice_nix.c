
#include <android/log.h>
#include <jni.h>
#include <stdbool.h>
#include <unistd.h>
#include <stdio.h>

extern void Java_ru0xdc_rtkgps_RtkNaviService_rtkSrvInit(JNIEnv* env, jclass clazz);
extern jboolean Java_ru0xdc_rtkgps_RtkNaviService_rtkSrvStart(JNIEnv* env, jclass clazz);

int main(int argc, const char **argv)
{
   printf("init");
   Java_ru0xdc_rtkgps_RtkNaviService_rtkSrvInit(NULL, NULL);
   printf("start");
   Java_ru0xdc_rtkgps_RtkNaviService_rtkSrvStart(NULL, NULL);

   while(1) { sleep(5); }

}


