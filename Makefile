#
# Variables
#

ifeq ($(OS), Windows_NT)
	CC = gcc-3
	CFLAGS = -mno-cygwin -Wall -g -O2
	LDFLAGS = -lws2_32
else
	CC = gcc
	CFLAGS = -Wall -g -O2
	LDFLAGS =
endif

RM = rm -f
SRCDIR = src/main/c
DESTDIR = target
OBJS =  groovyclient.o \
		buf.o \
		option.o \
		session.o \
		base64.o

#
# Rules
#

.PHONY: clean

groovyclient : $(OBJS)
	cd $(DESTDIR); $(CC) $(CFLAGS) -o $@ $(OBJS) $(LDFLAGS)

groovyclient.o : $(SRCDIR)/groovyclient.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $(DESTDIR)/$@ -c $<

buf.o :  $(SRCDIR)/buf.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $(DESTDIR)/$@ -c $<

option.o : $(SRCDIR)/option.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $(DESTDIR)/$@ -c $<

session.o : $(SRCDIR)/session.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $(DESTDIR)/$@ -c $<

base64.o : $(SRCDIR)/base64.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $(DESTDIR)/$@ -c $<

clean:
	$(RM) $(DESTDIR)/*.o $(DESTDIR)/groovyclient

