include $(CLEAR_VARS)
LOCAL_MODULE := gdal
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../gdal-prebuilt/$(APP_ABI)/include
#LOCAL_SRC_FILES := $(LOCAL_PATH)/../gdal-prebuilt/$(APP_ABI)/lib/libgdal.so
LOCAL_SRC_FILES := $(LOCAL_PATH)/../gdal-prebuilt/$(APP_ABI)/lib/libgdal.a

LOCAL_EXPORT_LDLIBS := -lz
LOCAL_EXPORT_LDLIBS += -lc
LOCAL_EXPORT_LDLIBS += -lm
LOCAL_EXPORT_LDLIBS += -lstdc++

include $(PREBUILT_STATIC_LIBRARY)
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := gdalalljni
LOCAL_SRC_FILES := gdal_wrap.cpp gdalconst_wrap.c ogr_wrap.cpp osr_wrap.cpp gnm_wrap.cpp
#LOCAL_SHARED_LIBRARIES := gdal
LOCAL_STATIC_LIBRARIES := gdal
include $(BUILD_SHARED_LIBRARY)

