LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/rtklib.mk

include $(CLEAR_VARS)

LOCAL_MODULE    := rtkgps

LOCAL_CFLAGS += -fvisibility=hidden

LOCAL_SRC_FILES := \
        rtkserver.c

LOCAL_STATIC_LIBRARIES := rtklib

include $(BUILD_SHARED_LIBRARY)

