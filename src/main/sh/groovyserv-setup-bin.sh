#!/bin/bash
#
# Copyright 2009-2014 the original author or authors.
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

select_os_arch() {
    local ext os arch
    case `uname` in
        Darwin)
            os=darwin
            ;;
        CYGWIN*)
            os=windows
            ext=.exe
            ;;
        *)
            os=linux
            ;;
    esac
    case `uname -m` in
        "x86_64" | "i686")
            arch=amd64
            ;;
        *)
            arch=386
            ;;
    esac
    echo "${os}_${arch}"
}

copy_bin() {
    local platform=$1
    local from_dir="$GROOVYSERV_HOME/platforms/$platforms"
    local bin_dir="$GROOVYSERV_HOME/bin"

    if [ ! -d "$from_dir" ]; then
        die "ERROR: your platform not supported: $platform"
    fi
    mkdir -p "$bin_dir" 2>/dev/null
    cp -r "$from_dir/"* "$bin_dir"
    chmod +x "$bin_dir/$bin_name"

    info_log
    info_log "Successfully setup bin from $from_dir"
}

#-------------------------------------------
# Main
#-------------------------------------------

copy_bin `select_os_arch`
