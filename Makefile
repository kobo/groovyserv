#
# Copyright 2014 the original author or authors.
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

# Specified from caller
OS_ARCH = local
EXT =
GROOVYSERV_VERSION = X.XX-SNAPSHOT

# Fixed
RM = rm -rf
GOCMD = go
SRCDIR = src/main/go
DESTDIR = build/platforms
LDFLAGS = -X main.GroovyServVersion $(GROOVYSERV_VERSION)

#
# Rules
#

.PHONY: clean

all: groovyserver groovyclient

%: $(SRCDIR)/%.go
	$(GOCMD) build --ldflags "$(LDFLAGS)" -o $(DESTDIR)/$(OS_ARCH)/$@$(EXT) $<


clean:
	$(RM) $(DESTDIR)

