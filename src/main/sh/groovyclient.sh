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
# Check GROOVYSERV_HOME and GROOVYSERVER_CMD
#-------------------------------------------

GROOVYSERVER_CMD="$GROOVYSERV_HOME/bin/groovyserver"
if ! is_command_avaiable "$GROOVYSERVER_CMD"; then
    error_log "ERROR: Not found 'groovyserver' command: $GROOVYSERVER_CMD"
    exit 1
fi

# ------------------------------------------
# GroovyServ's work directory
# ------------------------------------------

if [ ! -d "$GROOVYSERV_WORK_DIR" ]; then
    mkdir -p "$GROOVYSERV_WORK_DIR"
fi

#-------------------------------------------
# Common functions
#-------------------------------------------

usage() {
    echo "usage: `basename $0` [options]"
    echo "options:"
    echo "  -Ch,-Chelp                       show this usage"
    echo "  -Cs,-Chost                       specify the host to connect to groovyserver"
    echo "  -Cp,-Cport <port>                specify the port to connect to groovyserver"
    echo "  -Ca,-Cauthtoken <authtoken>      specify the authtoken"
    echo " (-Ck,-Ckill-server                unsupported in limited script)"
    echo " (-Cr,-Crestart-server             unsupported in limited script)"
    echo "  -Cq,-Cquiet                      suppress statring messages"
    echo "  -Cenv <substr>                   pass environment variables of which a name"
    echo "                                   includes specified substr"
    echo "  -Cenv-all                        pass all environment variables"
    echo "  -Cenv-exclude <substr>           don't pass environment variables of which a"
    echo "                                   name includes specified substr"
    echo "  -Cv,-Cversion                    display the GroovyServ version"
    echo
    echo "********************************** NOTE ***************************************"
    echo "  This client scirpt is LIMITED EDITION. So following features are unavailable:"
    echo "    * Transparent server operations (only starting server is available)"
    echo "    * Signal handling on client side (Ctrl+C)"
    echo "    * System.in from client"
    echo "    * Distinguishable stdout from stderr on client (all responses to stdout)"
    echo "    * Status code from server (\$?)"
    echo
    echo "  If you want to use a client of FULL EDITION:"
    echo "    * Use a ruby client 'groovyclient.rb'"
    echo "    * Download a native client for your environment:"
    echo "        http://kobo.github.io/groovyserv/download.html"
    echo "    * Build a naitive client on your own:"
    echo "        http://kobo.github.io/groovyserv/howtobuild.html"
    echo "*******************************************************************************"
}

version() {
    echo "GroovyServ Version: Client: @GROOVYSERV_VERSION@ (.sh) [Limited Edition]"
}

start_server() {
    # To try only for localhost
    [ "$GROOVYSERVER_HOST" != "localhost" ] && return

    if ! is_port_listened $GROOVYSERVER_PORT; then
        info_log "Invoking server: '$GROOVYSERVER_CMD' -p $GROOVYSERVER_PORT"
        $GROOVYSERVER_CMD
        if [ ! $? -eq 0 ]; then
            echo "ERROR: Sorry, unexpected error occurs"
            exit 1
        fi
    fi
}

send_request() {
    # To avoid complicated protocol for shellscript
    echo "Protocol: simple"

    # CWD/PWD on client side
    echo "Cwd: $PWD"

    # CLASSPATH must be always propagated
    if [ -n "$CLASSPATH" ]; then
        echo "Cp: $CLASSPATH"
    fi

    # Environment variable
    if [ -n "$ENV_ALL" ] || [ "${#ENV_INCLUDES[@]}" -gt 0 ]; then
        env | while read -r env_item
        do
            array=($(echo $env_item | sed -e "s/=/ /"))
            env_name=${array[0]}
            #env_value=${array[1]}

            # -Cenv-exclude
            local excluded=false
            for should_exclude_key in "${ENV_EXCLUDES[@]}"; do
                if [[ "$env_name" = *"$should_exclude_key"* ]]; then
                    excluded=true
                    break
                fi
            done
            $excluded && continue

            # -Cenv-all / -Cenv
            local included=false
            if [ -n "$ENV_ALL" ]; then
                included=true
            else
                for should_include_key in "${ENV_INCLUDES[@]}"; do
                    if [[ "$env_name" = *"$should_include_key"* ]]; then
                        included=true
                        break
                    fi
                done
            fi
            $included || continue

            echo "Env: $env_item"
        done
    fi

    # Authtoken
    echo "AuthToken: ${AUTHTOKEN:-$(cat "$GROOVYSERV_AUTHTOKEN_FILE")}"

    # Arguments for groovy command
    for arg in "${SERVER_OPTIONS[@]}"; do
        echo "Arg: $(printf "%s" "$arg" | base64 | tr -d '\n')"
    done

    # End Of Request
    echo ""
}

start_session() {
    # Connect to server
    exec 5<> /dev/tcp/$GROOVYSERVER_HOST/$GROOVYSERVER_PORT

    # Send request
    [ -n "$DEBUG" ] && send_request
    send_request >&5

    # Output response
    #perl -pe 'local $|=1; $line' <&5
    cat <&5

    # For combination of groovyserver and groovyclient
    if [ -n "$SHOULD_SHOW_VERSION_LATER" ]; then version; fi
    if [ -n "$SHOULD_SHOW_USAGE_LATER" ];   then echo; usage; fi
}


#-------------------------------------------
# Main
#-------------------------------------------

# Parse arguments
SERVER_OPTIONS=()
ENV_INCLUDES=()
ENV_EXCLUDES=()
while [ $# -gt 0 ]; do
    case $1 in
        -Chelp | -Ch)
            usage
            exit 0
            ;;
        --help | -h)
            SERVER_OPTIONS+=("$1")
            SHOULD_SHOW_USAGE_LATER=true
            shift
            ;;
        -Cversion | -Cv)
            version
            exit 0
            ;;
        --version | -v*)
            SERVER_OPTIONS+=("$1")
            SHOULD_SHOW_VERSION_LATER=true
            shift
            ;;
        -q)
            SERVER_OPTIONS+=("$1")
            QUIET=true
            shift
            ;;
        -Chost | -Cs)
            shift
            GROOVYSERVER_HOST=$1
            shift
            ;;
        -Cport | -Cp)
            shift
            GROOVYSERVER_PORT=$1
            shift
            ;;
        -Cauthtoken | -Ca)
            shift
            AUTHTOKEN=$1
            shift
            ;;
        -Cenv-all)
            ENV_ALL=true
            shift
            ;;
        -Cenv)
            shift
            ENV_INCLUDES+=("$1")
            shift
            ;;
        -Cenv-exclude)
            shift
            ENV_EXCLUDES+=("$1")
            shift
            ;;
        -Ckill-server | -Ck)
            die "Unsupported in limited script"
            ;;
        -Crestart-server | -Cr)
            die "Unsupported in limited script"
            ;;
        *)
            SERVER_OPTIONS+=("$1")
            shift
    esac
done

# Display additionally client's usage at the end of session
if [ "${#SERVER_OPTIONS[@]}" -eq 0 ]; then
    SHOULD_SHOW_USAGE_LATER=true
fi

# Start server if necessary
start_server

# Request to server
start_session

