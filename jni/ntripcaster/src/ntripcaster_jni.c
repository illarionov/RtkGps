#include <android/log.h>
#include <jni.h>
#include <strings.h>
#include <pthread.h>
#include <config.h>

char *appPath = NULL;
pthread_t ntripcaster_thread;
int process_id = 0;

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    (void)reserved;

    __android_log_print(ANDROID_LOG_VERBOSE, TAG, "Entering JNI_OnLoad");

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK)
        goto bail;

    if (!registerNTRIPCasterNatives(env))
        goto bail;

    /* success -- return valid version number */
    result = JNI_VERSION_1_6;

bail:
	__android_log_print(ANDROID_LOG_VERBOSE, TAG, "Leaving JNI_OnLoad (result=0x%x)", result);
    return result;
}

void *_serverstart()
{
	android_log(ANDROID_LOG_VERBOSE,"ntripcaster status before starting:%d",get_ntripcaster_state());
	set_run_path(appPath);
	thread_lib_init ();
	init_thread_tree (__LINE__, __FILE__);
	setup_defaults ();
	setup_signal_traps ();
	allocate_resources ();
	init_authentication_scheme ();
	parse_default_config_file ();
	initialize_network ();
	startup_mode ();
	android_log(ANDROID_LOG_VERBOSE,"ntripcaster status after ending:%d",get_ntripcaster_state());
	pthread_exit(0);
	android_log(ANDROID_LOG_VERBOSE,"ntripcaster ended");
}

static jint NTRIPCaster_serverstart(JNIEnv* env, jclass clazz, jint port, jstring conf_filename)
{
	jint ret = 0;
	const char *filename = (*env)->GetStringUTFChars(env, conf_filename, 0);
	(*env)->ReleaseStringUTFChars(env,conf_filename, filename);
	if(pthread_create(&ntripcaster_thread, NULL, &_serverstart, NULL)) {

		android_log(ANDROID_LOG_ERROR,"Error creating thread\n");
		return (jint)-1;

	}

	return (jint)ret;
}

static jint NTRIPCaster_serverstop(JNIEnv* env, jclass clazz,jint force)
{

	if (force)
	{
		android_log(ANDROID_LOG_VERBOSE,"ntripcaster brutal ending...");
		pthread_kill(ntripcaster_thread, SIGKILL);
		return (jint)-2;
	}

	android_log(ANDROID_LOG_VERBOSE,"ntripcaster status:%d",get_ntripcaster_state());
	kick_everything ();
	pthread_kill(ntripcaster_thread, SIGINT);
	if (pthread_join_timeout(ntripcaster_thread,1000))
	{
		android_log(ANDROID_LOG_VERBOSE,"ntripcaster was not cleanly ended");
		return (jint)-1;
	}
	return (jint)0;

}

static jint NTRIPCaster_serverreset(JNIEnv* env, jclass clazz)
{

	return (jint)pthread_kill(ntripcaster_thread, SIGHUP);

}

static jstring NTRIPCaster_getVersion(JNIEnv* env, jclass clazz){
	jstring result;
	result = (*env)->NewStringUTF(env,VERSION);
	return result;
}

JNIEXPORT void JNICALL NTRIPCaster_setApplicationPath
  (JNIEnv *env, jclass class, jstring applicationPath)
{
    const char *givenPath = (*env)->GetStringUTFChars(env, applicationPath, NULL);
    if (!givenPath) return ;
    android_log(ANDROID_LOG_VERBOSE,"GivenPath: %s", givenPath);
    if (appPath) free(appPath);
    appPath = (char*) malloc( (strlen(givenPath)+1)*sizeof(char) );
    if (!appPath) return;
    strcpy(appPath,givenPath);
    (*env)->ReleaseStringUTFChars(env, applicationPath, givenPath);

}

char * getApplicationPath() {
	return appPath;
}

static JNINativeMethod nativeMethods[] = {
   {"start", "(ILjava/lang/String;)I",(void*)NTRIPCaster_serverstart},
   {"stop", "(I)I",(void*)NTRIPCaster_serverstop},
   {"reset", "()V",(void*)NTRIPCaster_serverreset},
   {"getVersion","()Ljava/lang/String;",(void*)NTRIPCaster_getVersion},
   {"setApplicationPath","(Ljava/lang/String;)V",(void*)NTRIPCaster_setApplicationPath}
};

int registerNTRIPCasterNatives(JNIEnv* env) {
    int result = -1;

    /* look up the class */
    jclass clazz = (*env)->FindClass(env, "gpsplus/ntripcaster/NTRIPCaster");

    if (clazz == NULL)
       return JNI_FALSE;

    if ((*env)->RegisterNatives(env, clazz, nativeMethods, sizeof(nativeMethods)
	     / sizeof(nativeMethods[0])) != JNI_OK)
       return JNI_FALSE;

    return JNI_TRUE;
}
