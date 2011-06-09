#
# Variables
#

CC = gcc
CFLAGS = -Wall -g -O2
RM = rm -f
SRCDIR = src/main/c
DESTDIR = target
OBJS =  $(DESTDIR)/groovyclient.o \
		$(DESTDIR)/buf.o \
		$(DESTDIR)/option.o \
		$(DESTDIR)/session.o \
		$(DESTDIR)/base64.o


#
# Rules
#

groovyclient : $(OBJS)
	$(CC) -o $(DESTDIR)/$@ $(OBJS)

$(DESTDIR)/groovyclient.o : $(SRCDIR)/groovyclient.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $@ -c $<

$(DESTDIR)/buf.o :  $(SRCDIR)/buf.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $@ -c $<

$(DESTDIR)/option.o : $(SRCDIR)/option.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $@ -c $<

$(DESTDIR)/session.o : $(SRCDIR)/session.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $@ -c $<

$(DESTDIR)/base64.o : $(SRCDIR)/base64.c $(SRCDIR)/*.h
	$(CC) $(CFLAGS) -o $@ -c $<

#
# Helper
#

.PHONY: all clean

all: groovyclient

clean:
	$(RM) $(DESTDIR)/*.o $(DESTDIR)/groovyclient

