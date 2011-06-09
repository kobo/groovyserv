#
# Copyright 2009-2011 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Variables
#

ifeq ($(OS), Windows_NT)
	CC = gcc-3
	CFLAGS = -mno-cygwin -Wall -g
	LDFLAGS = -lws2_32
else
	CC = gcc
	CFLAGS = -Wall -g
	LDFLAGS =
endif

RM = rm -f
MKDIR = mkdir -p
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

.PHONY: clean

groovyclient:: $(OBJS)
	$(CC) $(CFLAGS) -o $(DESTDIR)/$@ $(OBJS) $(LDFLAGS)

$(DESTDIR)/groovyclient.o: $(SRCDIR)/groovyclient.c $(SRCDIR)/*.h

$(DESTDIR)/buf.o: $(SRCDIR)/buf.c $(SRCDIR)/*.h

$(DESTDIR)/option.o: $(SRCDIR)/option.c $(SRCDIR)/*.h

$(DESTDIR)/session.o: $(SRCDIR)/session.c $(SRCDIR)/*.h

$(DESTDIR)/base64.o: $(SRCDIR)/base64.c $(SRCDIR)/*.h

$(DESTDIR)/%.o: $(SRCDIR)/%.c $(SRCDIR)/*.h
	@$(MKDIR) $(DESTDIR)
	$(CC) $(CFLAGS) -o $@ -c $<

clean:
	$(RM) $(DESTDIR)/*.o $(DESTDIR)/groovyclient

