include $(CLEAR_VARS)

RTKLIB_PATH := rtklib_2.4.2b10
RTKLIB_CFLAGS := -DENAGLO -DENAGAL -DENAQZS -DENACMP -DNFREQ=3 -DTRACE

LOCAL_MODULE    := rtklib

LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += $(RTKLIB_CFLAGS)
LOCAL_EXPORT_CFLAGS := $(RTKLIB_CFLAGS)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(RTKLIB_PATH)/src
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(RTKLIB_PATH)/src

LOCAL_LDLIBS += -llog
LOCAL_EXPORT_LDLIBS += -llog

LOCAL_SRC_FILES := \
        $(RTKLIB_PATH)/src/convkml.c \
        $(RTKLIB_PATH)/src/convrnx.c \
        $(RTKLIB_PATH)/src/datum.c \
        $(RTKLIB_PATH)/src/download.c \
        $(RTKLIB_PATH)/src/ephemeris.c \
        $(RTKLIB_PATH)/src/geoid.c \
        $(RTKLIB_PATH)/src/ionex.c \
        $(RTKLIB_PATH)/src/lambda.c \
        $(RTKLIB_PATH)/src/options.c \
        $(RTKLIB_PATH)/src/pntpos.c \
        $(RTKLIB_PATH)/src/postpos.c \
        $(RTKLIB_PATH)/src/ppp_ar.c \
        $(RTKLIB_PATH)/src/ppp.c \
        $(RTKLIB_PATH)/src/preceph.c \
        $(RTKLIB_PATH)/src/qzslex.c \
        $(RTKLIB_PATH)/src/rcvraw.c \
        $(RTKLIB_PATH)/src/rinex.c \
        $(RTKLIB_PATH)/src/rtcm2.c \
        $(RTKLIB_PATH)/src/rtcm3.c \
        $(RTKLIB_PATH)/src/rtcm3e.c \
        $(RTKLIB_PATH)/src/rtcm.c \
        $(RTKLIB_PATH)/src/rtkcmn.c \
        $(RTKLIB_PATH)/src/rtkpos.c \
        $(RTKLIB_PATH)/src/rtkpos_gsi.c \
        $(RTKLIB_PATH)/src/rtksvr.c \
        $(RTKLIB_PATH)/src/sbas.c \
        $(RTKLIB_PATH)/src/solution.c \
        $(RTKLIB_PATH)/src/stec.c \
        $(RTKLIB_PATH)/src/stream.c \
        $(RTKLIB_PATH)/src/streamsvr.c \
        $(RTKLIB_PATH)/src/tle.c

LOCAL_SRC_FILES += \
	$(RTKLIB_PATH)/src/rcv/crescent.c \
	$(RTKLIB_PATH)/src/rcv/gw10.c \
        $(RTKLIB_PATH)/src/rcv/javad.c \
        $(RTKLIB_PATH)/src/rcv/novatel.c \
        $(RTKLIB_PATH)/src/rcv/nvs.c \
        $(RTKLIB_PATH)/src/rcv/rcvlex.c \
        $(RTKLIB_PATH)/src/rcv/skytraq.c \
        $(RTKLIB_PATH)/src/rcv/ss2.c \
        $(RTKLIB_PATH)/src/rcv/ublox.c

LOCAL_SRC_FILES += log.c

#LOCAL_SRC_FILES += \
#        rtknaviservice.c \

TARGET-process-src-files-tags += $(call add-src-files-target-cflags, \
   $(RTKLIB_PATH)/src/download.c, -DS_IREAD=S_IRUSR)

include $(BUILD_STATIC_LIBRARY)

