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

# only for groovyserver
if $OS_CYGWIN; then
    GROOVY_HOME=`cygpath --unix --ignore "$GROOVY_HOME"`
    GROOVYSERV_HOME=`cygpath --unix --ignore "$GROOVYSERV_HOME"`
    CLASSPATH=`cygpath --unix --ignore --path "$CLASSPATH"`
fi

#-------------------------------------------
# Find groovy command
#-------------------------------------------

if [ "$GROOVY_HOME" != "" ]; then
    info_log "Groovy home directory: $GROOVY_HOME"
    GROOVY_BIN="$GROOVY_HOME/bin/groovy"
    if [ ! -x "$GROOVY_BIN" ]; then
        error_log "ERROR: Not found a groovy command in GROOVY_HOME: $GROOVY_BIN"
        exit 1
    fi
    info_log "Groovy command path: $GROOVY_BIN (found at GROOVY_HOME)"
elif is_command_avaiable groovy; then
    info_log "Groovy home directory: (none)"
    GROOVY_BIN=`which groovy`
    info_log "Groovy command path: $GROOVY_BIN (found at PATH)"
else
    error_log "ERROR: Not found a groovy command. Required either PATH having groovy command or GROOVY_HOME"
    exit 1
fi

#-------------------------------------------
# Check GROOVYSERV_HOME
#-------------------------------------------

if ! is_file_exists "$GROOVYSERV_HOME/lib/groovyserv-*.jar"; then
    error_log "ERROR: Not found a valid GROOVYSERV_HOME directory: $GROOVYSERV_HOME"
    exit 1
fi
info_log "GroovyServ home directory: $GROOVYSERV_HOME"

# ------------------------------------------
# GroovyServ's work directory
# ------------------------------------------

if [ ! -d "$GROOVYSERV_WORK_DIR" ]; then
    mkdir -p "$GROOVYSERV_WORK_DIR"
fi
info_log "GroovyServ work directory: $GROOVYSERV_WORK_DIR"

#-------------------------------------------
# Port and PID and AuthToken
#-------------------------------------------

GROOVYSERV_OPTS="$GROOVYSERV_OPTS -Dgroovyserver.port=$GROOVYSERVER_PORT"

#-------------------------------------------
# Common functions
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

is_pid_file_expired() {
    local result=$(cd "$GROOVYSERV_WORK_DIR" && find . -name $(basename $GROOVYSERV_PID_FILE) -mmin +1)
    [ "$result" != "" ]
}

#-------------------------------------------
# Parse arguments
#-------------------------------------------

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
            GROOVYSERVER_PORT=$1
            shift
            ;;
        -k)
            DO_KILL=KILL_ONLY
            shift
            ;;
        -r)
            DO_KILL=RESTART
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

#-------------------------------------------
# Setup classpath
#-------------------------------------------

info_log "Original classpath: ${CLASSPATH:-(none)}"
if [ "$CLASSPATH" = "" ]; then
    export CLASSPATH="$GROOVYSERV_HOME/lib/*"
else
    export CLASSPATH="${GROOVYSERV_HOME}/lib/*:${CLASSPATH}"
fi
info_log "GroovyServ default classpath: $CLASSPATH"

#-------------------------------------------
# Setup other variables
#-------------------------------------------

# -server option for JVM (for performance) (experimental)
export JAVA_OPTS="$JAVA_OPTS -server"

#-------------------------------------------
# Kill process if specified
#-------------------------------------------

if [ "$DO_KILL" != "" ]; then
    if [ -f "$GROOVYSERV_PID_FILE" ]; then
        EXISTED_PID=`cat "$GROOVYSERV_PID_FILE"`
        ps -p $EXISTED_PID >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            kill -9 $EXISTED_PID
            info_log "Killed groovyserver of $EXISTED_PID($GROOVYSERVER_PORT)"
        else
            info_log "Process of groovyserver of $EXISTED_PID($GROOVYSERVER_PORT) not found"
        fi
        rm -f "$GROOVYSERV_PID_FILE"
        rm -f "$GROOVYSERV_AUTHTOKEN_FILE"
    else
        info_log "PID file $GROOVYSERV_PID_FILE not found"
    fi
    if [ "$DO_KILL" = "KILL_ONLY" ]; then
        exit 0
    fi
    info_log "Restarting groovyserver"
fi

#-------------------------------------------
# Check duplicated invoking
#-------------------------------------------

if [ -f "$GROOVYSERV_PID_FILE" ]; then
    EXISTED_PID=`cat "$GROOVYSERV_PID_FILE"`

    # if connecting to server is succeed, return with warning message
    if is_port_listened $GROOVYSERVER_PORT; then
        error_log "WARN: groovyserver is already running as $EXISTED_PID($GROOVYSERVER_PORT)"
        exit 1
    fi

    # if PID file doesn't expired, terminate the sequence of invoking server
    if ! is_pid_file_expired; then
        error_log "WARN: Another process may be starting groovyserver."
        exit 1
    fi
fi

#-------------------------------------------
# Invoke server
#-------------------------------------------

if [ "$DEBUG" != "" ]; then
    echo "Invoking server for DEBUG..."
    echo "$GROOVY_BIN" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"
    "$GROOVY_BIN" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"
    exit 0
else
    nohup "$GROOVY_BIN" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)" > /dev/null 2>&1 &
fi

#-------------------------------------------
# Store PID
#-------------------------------------------

sleep 1
PID=$!
ps -p $PID | grep $PID > /dev/null
if [ $? -eq 0 ]; then
    echo $PID > "$GROOVYSERV_PID_FILE"
else
    error_log "ERROR: Failed to store PID into file $GROOVYSERV_PID_FILE"
    error_log "Rerun for debug..."
    "$GROOVY_BIN" $GROOVYSERV_OPTS -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)" &
    exit 1
fi

#-------------------------------------------
# Wait for available
#-------------------------------------------

if [ ! $QUIET ]; then
    /bin/echo -n "Starting" 1>&2
fi

while true; do
    if [ ! $QUIET ]; then
        /bin/echo -n "." 1>&2
    fi
    sleep 1

    # waiting until authToken filed is created
    if [ ! -f "$GROOVYSERV_AUTHTOKEN_FILE" ]; then
        continue
    fi

    # if connecting to server is succeed, return successfully
    if is_port_listened $GROOVYSERVER_PORT; then
        break
    fi

    # if PID file was expired while to connect to server is failing, error
    if is_pid_file_expired; then
        error_log "ERROR: Timeout. Confirm if groovyserver $PID($GROOVYSERVER_PORT) is running."
        exit 1
    fi
done

info_log
info_log "groovyserver $PID($GROOVYSERVER_PORT) is successfully started"

