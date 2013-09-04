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

usage() {
    echo "usage: `basename $0` [options]"
    echo "options:"
    echo "  -v                       verbose output to the log file"
    echo "  -q                       suppress starting messages"
    echo "  -k                       kill the running groovyserver"
    echo "  -r                       restart the running groovyserver"
    echo "  -p <port>                specify the port to listen"
    echo "  --allow-from <addresses> specify optional acceptable client addresses (delimiter: comma)"
    echo "  --authtoken <authtoken>  specify authtoken (which is automatically generated if not specified)"
}

find_groovy_command() {
    unset GROOVY_CMD
    if [ "$GROOVY_HOME" != "" ]; then
        info_log "Groovy home directory: $GROOVY_HOME"
        GROOVY_CMD="$GROOVY_HOME/bin/groovy"
        if [ ! -x "$GROOVY_CMD" ]; then
            die "ERROR: Not found a groovy command in GROOVY_HOME: $GROOVY_CMD"
        fi
        info_log "Groovy command path: $GROOVY_CMD (found at GROOVY_HOME)"
    elif is_command_avaiable groovy; then
        info_log "Groovy home directory: (none)"
        GROOVY_CMD=`which groovy`
        info_log "Groovy command path: $GROOVY_CMD (found at PATH)"
    else
        die "ERROR: Not found a groovy command. Required either PATH having groovy command or GROOVY_HOME"
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
    # -server option for JVM (for performance) (experimental)
    export JAVA_OPTS="$JAVA_OPTS -server"
}

send_shutdown_request() {
    echo "Auth: ${AUTHTOKEN:-$(cat "$GROOVYSERV_AUTHTOKEN_FILE")}"
    echo "Cmd: shutdown"
}

kill_process_if_specified() {
    if $DO_KILL || $DO_RESTART; then
        if [ -f "$GROOVYSERV_AUTHTOKEN_FILE" ]; then
            $DEBUG && send_shutdown_request
            send_shutdown_request | nc localhost $GROOVYSERV_PORT
        else
            info_log "AuthToken file $GROOVYSERV_AUTHTOKEN_FILE not found"
        fi
        if $DO_KILL; then
            exit 0
        fi
        info_log "Restarting groovyserver"
    fi
}

check_duplicated_invoking() {
    # if connecting to server is succeed, return with warning message
    if is_port_listened $GROOVYSERV_PORT; then
        die "WARN: groovyserver is already running on port $GROOVYSERV_PORT"
    fi
}

invoke_server() {
    if $DEBUG; then
        echo "Invoking server for DEBUG..."
        echo "$GROOVY_CMD" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"
        "$GROOVY_CMD" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"
        exit 0
    else
        nohup "$GROOVY_CMD" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)" > /dev/null 2>&1 &
    fi
}

wait_for_server_available() {
    if ! ${QUIET}; then
        /bin/echo -n "Starting" 1>&2
    fi

    while true; do
        if ! ${QUIET}; then
            /bin/echo -n "." 1>&2
        fi
        sleep 1

        # waiting until authToken filed is created
        [ ! -f "$GROOVYSERV_AUTHTOKEN_FILE" ] && continue

        # if connecting to server is succeed, return successfully
        is_port_listened $GROOVYSERV_PORT && break
    done

    info_log
    info_log "groovyserver is successfully started on $GROOVYSERV_PORT port"
}

# ------------------------------------------
# Setup global variables only for here
# ------------------------------------------

DO_KILL=false
DO_RESTART=false

#-------------------------------------------
# Main
#-------------------------------------------

# Parse arguments
while [ $# -gt 0 ]; do
    case $1 in
        -v)
            GROOVYSERV_OPTS="$GROOVYSERV_OPTS -Dgroovyserver.verbose=true"
            shift
            ;;
        -q)
            QUIET=true
            shift
            ;;
        -p)
            shift
            GROOVYSERV_PORT=$1
            shift
            ;;
        -k)
            DO_KILL=true
            shift
            ;;
        -r)
            DO_RESTART=true
            shift
            ;;
        --allow-from)
            shift
            GROOVYSERV_OPTS="$GROOVYSERV_OPTS -Dgroovyserver.allowFrom=$1"
            shift
            ;;
        --authtoken)
            shift
            GROOVYSERV_OPTS="$GROOVYSERV_OPTS -Dgroovyserver.authtoken=$1"
            shift
            ;;
        *)
            usage
            exit 1
            ;;
    esac
done

# Decide Port/AuthToken (it must be after resolving port number from arguments)
GROOVYSERV_OPTS="$GROOVYSERV_OPTS -Dgroovyserver.port=${GROOVYSERV_PORT}"
GROOVYSERV_AUTHTOKEN_FILE=$(get_authtoken_file)

# Pre-processing
find_groovy_command
check_groovyserv_home
setup_classpath
setup_java_opts

# Server operation
kill_process_if_specified
check_duplicated_invoking
invoke_server

# Post-processing
wait_for_server_available

