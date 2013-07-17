#!/bin/bash
#
# Copyright 2009-2013 the original author or authors.
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

# resolve links - $0 may be a soft-link
PRG="$0"

while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done

DIRNAME=`dirname "$PRG"`

#-------------------------------------------
# Load common settings
#-------------------------------------------

. "$DIRNAME/_common.sh"

#-------------------------------------------
# Check GROOVYSERV_HOME
#-------------------------------------------

if ! is_file_exists "$GROOVYSERV_HOME/native/src/main/c"; then
    error_log "ERROR: Not found a valid GROOVYSERV_HOME directory: $GROOVYSERV_HOME"
    exit 1
fi

#-------------------------------------------
# Check make command
#-------------------------------------------

if ! is_command_avaiable make; then
    die "ERROR: Not found 'make' command. This script is required 'make' and 'gcc' command to build a native client from source."
fi

#-------------------------------------------
# Main
#-------------------------------------------

# Build
SRC_DIR="$GROOVYSERV_HOME/native"
info_log "Source directory: $SRC_DIR"
cd $SRC_DIR
make clean
make -e GROOVYSERV_VERSION="@GROOVYSERV_VERSION@"

# Install
BUILT_CLIENT_PATH="$SRC_DIR/build/natives/groovyclient"
BIN_CLIENT_PATH="$GROOVYSERV_HOME/bin/groovyclient"
cp -f "$BUILT_CLIENT_PATH" "$BIN_CLIENT_PATH"
chmod +x "$BIN_CLIENT_PATH"

info_log
info_log "Successfully installed: $BIN_CLIENT_PATH"

