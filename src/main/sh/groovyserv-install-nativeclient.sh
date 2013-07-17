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
# Functions
#-------------------------------------------

check_groovyserv_home() {
    if ! is_file_exists "$GROOVYSERV_HOME/native/src/main/c"; then
        die "ERROR: Not found a valid GROOVYSERV_HOME directory: $GROOVYSERV_HOME"
    fi
}

check_make_command() {
    if ! is_command_avaiable make; then
        die "ERROR: Not found 'make' command. This script is required 'make' and 'gcc' command to build a native client from source."
    fi
}

build_nativeclient() {
    local src_dir="$GROOVYSERV_HOME/native"
    info_log "Source directory: $src_dir"

    cd "$src_dir"
    make clean
    make -e GROOVYSERV_VERSION="@GROOVYSERV_VERSION@"
}

install_nativeclient() {
    local src_dir="$GROOVYSERV_HOME/native"
    local built_client_path="$src_dir/build/natives/groovyclient"
    local bin_client_path="$GROOVYSERV_HOME/bin/groovyclient"

    if [ -f "$bin_client_path" ]; then
        # explicitly delete the 'groovyclinet' file because 'groovyclient.exe' cannot overwrite the file in windows
        rm -f "$bin_client_path"
    fi
    cp "$built_client_path" "$bin_client_path"
    chmod +x "$bin_client_path"

    info_log
    info_log "Successfully installed: $bin_client_path"
}

#-------------------------------------------
# Main
#-------------------------------------------

# Pre-processing
check_groovyserv_home
check_make_command

# Building and installing
build_nativeclient
install_nativeclient

