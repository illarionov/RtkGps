#include <android/log.h>
#include <stdarg.h>

#include "rtklib.h"

#define LOG_TAG "rtklib"

extern int showmsg(char *format,...) {
   int r;
   va_list ap;

   va_start(ap, format);
   r = __android_log_vprint(ANDROID_LOG_VERBOSE, LOG_TAG, format, ap);
   va_end(ap);
   return r;
}

extern void settspan(gtime_t ts, gtime_t te) {}
extern void settime(gtime_t time) {}

