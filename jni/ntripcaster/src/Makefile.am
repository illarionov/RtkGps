## Process this with automake to create Makefile.in

AUTOMAKE_OPTIONS = foreign

bin_PROGRAMS = ntripcaster

noinst_HEADERS = avl.h client.h	definitions.h connection.h	\
			ntrip_string.h ntripcaster.h log.h	main.h \
			sock.h source.h threads.h timer.h utility.h

ntripcaster_SOURCES = main.c client.c source.c connection.c log.c \
			sock.c threads.c utility.c avl.c timer.c ntrip_string.c

INCLUDES = -D_REENTRANT @WRAPINCLUDES@

bindir=$(NTRIPCASTER_BINDIR)

