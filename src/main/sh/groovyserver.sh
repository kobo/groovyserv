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

# only for groovyserver on cygwin
if $OS_CYGWIN; then
    GROOVY_HOME=`cygpath --unix --ignore "$GROOVY_HOME"`
    GROOVYSERV_HOME=`cygpath --unix --ignore "$GROOVYSERV_HOME"`
    CLASSPATH=`cygpath --unix --ignore --path "$CLASSPATH"`
fi

#-------------------------------------------
# Functions
#-------------------------------------------

find_groovy_command() {
    unset GROOVY_CMD
    if [ "$GROOVY_HOME" != "" ]; then
        info_log "Groovy home directory: $GROOVY_HOME"
        GROOVY_CMD="$GROOVY_HOME/bin/groovy"
        if [ ! -x "$GROOVY_CMD" ]; then
            die "ERROR: Not found a groovy command in GROOVY_HOME: $GROOVY_CMD"
        fi
        info_log "Groovy command path: $GROOVY_CMD (found at GROOVY_HOME)"
    elif is_command_available groovy; then
        info_log "Groovy home directory: (none)"
        GROOVY_CMD=`which groovy`
        info_log "Groovy command path: $GROOVY_CMD (found at PATH)"
    else
        die "ERROR: Not found a groovy command. Required either PATH having groovy command or GROOVY_HOME."
    fi
}

check_groovyserv_home() {
    if ! is_file_exists "$GROOVYSERV_HOME/lib/groovyserv-*.jar"; then
        die "ERROR: Not found a valid GROOVYSERV_HOME directory: $GROOVYSERV_HOME"
    fi
    info_log "GroovyServ home directory: $GROOVYSERV_HOME"
    info_log "GroovyServ work directory: $GROOVYSERV_WORK_DIR"
}

setup_classpath() {
    info_log "Original classpath: ${CLASSPATH:-(none)}"
    if [ "$CLASSPATH" = "" ]; then
        export CLASSPATH="$GROOVYSERV_HOME/lib/*"
    else
        export CLASSPATH="${GROOVYSERV_HOME}/lib/*:${CLASSPATH}"
    fi
    info_log "GroovyServ default classpath: $CLASSPATH"
}

setup_java_opts() {
    # -server: for performance (experimental)
    # -Djava.awt.headless=true: without this, annoying to switch an active process to it when new process is created as daemon
    export JAVA_OPTS="$JAVA_OPTS -server -Djava.awt.headless=true"
}

invoke_server() {
    local groovyserv_script_path="$(expand_path $0)"
    exec "$GROOVY_CMD" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.ui.ServerCLI.main(args)" -- "$groovyserv_script_path" "$@"
}

usage() {
    # when updating usage text, print ServerCLI's output and customize it.
    cat << EOF
usage: $(basename $0) [options]
options:
  -h,--help                     show this usage
  -k,--kill                     kill the running groovyserver
  -p,--port <port>              specify the port to listen
  -q,--quiet                    suppress output to console except error message
  -r,--restart                  restart the running groovyserver
  -v,--verbose                  verbose output to a log file
     --allow-from <addresses>   specify optional acceptable client addresses (delimiter: comma)
     --authtoken <authtoken>    specify authtoken (which is automatically generated if not specified)
EOF
}

#-------------------------------------------
# Main
#-------------------------------------------

# Parse arguments
for arg in "$@"; do
    # Must not consume original arguments because they needs for invoke_server
    case $arg in
        -q | --quiet)
            QUIET=true
            ;;
        -h | --help)
            usage
            exit 0
            ;;
    esac
done

# Pre-processing
find_groovy_command
check_groovyserv_home
setup_classpath
setup_java_opts

# Server operation
invoke_server "$@"
