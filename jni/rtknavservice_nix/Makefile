CC?=gcc
#CFLAGS?= -O3 -pipe -DNDEBUG
CFLAGS=-W -g -O0

RTKLIB_PATH := ../rtklib_2.4.2b10
RTKLIB_CFLAGS := -DENAGLO -DENAGAL -DENAQZS -DENACMP -DNFREQ=3 -DTRACE -DDLL

ANDROID_INCLUDES := \
   -idirafter $(ANDROID_NDK_ROOT)/platforms/android-14/arch-x86/usr/include

LDFLAGS += -lm -lpthread

all: rtknavservice_nix

clean:
	rm -f *.o rtknavservice_nix

rtknavservice_nix: rtknaviservice_nix.c ../rtknaviservice.c
	$(CC) \
	   -I$(RTKLIB_PATH)/src  \
	   $(ANDROID_INCLUDES) \
	   $(RTKLIB_CFLAGS) \
	   rtknaviservice_nix.c ../rtknaviservice.c \
	   $(RTKLIB_PATH)/src/*.c \
	   $(RTKLIB_PATH)/src/rcv/*.c \
	   $(LDFLAGS) -o rtknavservice_nix

