APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi armeabi-v7a mips x86
#APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/Android.mk
APP_PLATFORM := android-8

#NDK_TOOLCHAIN_VERSION=clang
NDK_MODULE_PATH=$(APP_PROJECT_PATH)/jni

#APP_OPTIM := debug
#APP_CFLAGS += -O3 -flto -fgraphite -fgraphite-identity
#APP_LDFLAGS += -flto -fgraphite -fgraphite-identity
