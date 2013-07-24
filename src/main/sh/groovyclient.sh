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

usage() {
    echo "usage: `basename $0` [options]"
    echo "options:"
    echo "  -Ch,-Chelp                       show this usage"
    echo "  -Cs,-Chost                       specify the host to connect to groovyserver"
    echo "  -Cp,-Cport <port>                specify the port to connect to groovyserver"
    echo "  -Ca,-Cauthtoken <authtoken>      specify the authtoken"
    echo "  -Ck,-Ckill-server                kill the running groovyserver"
    echo "  -Cr,-Crestart-server             restart the running groovyserver"
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
    echo "    * Signal handling on client side (Ctrl+C)"
    echo "    * System.in from client"
    echo "    * Distinguishable stdout from stderr on client (all responses to stdout)"
    echo "    * Status code from server (\$?)"
    echo
    echo "  If you want to use a client of FULL EDITION:"
    echo "    * Run 'groovyserv-install-native.sh' if you have make and gcc,"
    echo "      to build a native client and replace 'groovyclient' with it"
    echo "    * Use a ruby client 'groovyclient.rb'"
    echo "*******************************************************************************"
}

version() {
    echo "GroovyServ Version: Client: @GROOVYSERV_VERSION@ (.sh) [Limited Edition]"
}

check_environment() {
    SERVER_CMD="$GROOVYSERV_HOME/bin/groovyserver"
    if ! is_command_avaiable "$SERVER_CMD"; then
        die "ERROR: Not found 'groovyserver' command: $SERVER_CMD"
    fi
}

invoke_server_command() {
    info_log "Invoking server: '$SERVER_CMD' -p $GROOVYSERVER_PORT ${SERVER_OPTIONS[@]}"
    "$SERVER_CMD" -p $GROOVYSERVER_PORT ${SERVER_OPTIONS[@]} || die "ERROR: Sorry, unexpected error occurs"
}

start_server() {
    # To try only for localhost
    [ "$GROOVYSERVER_HOST" != "localhost" ] && return

    # Specified
    if $DO_RESTART || $DO_KILL; then
        invoke_server_command
        $DO_KILL && exit 0

    # Automatically
    elif ! is_port_listened $GROOVYSERVER_PORT; then
        invoke_server_command
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
    echo "AuthToken: ${AUTHTOKEN:-$(cat "$(get_authtoken_file)")}"

    # Arguments for groovy command
    for arg in "${SERVER_ARGS[@]}"; do
        echo "Arg: $(printf "%s" "$arg" | base64 | tr -d '\n')"
    done

    # End Of Request
    echo ""
}

start_session() {
    # Connect to server
    exec 5<> /dev/tcp/$GROOVYSERVER_HOST/$GROOVYSERVER_PORT

    # Send request
    $DEBUG && send_request
    send_request >&5

    # Output response
    #perl -pe 'local $|=1; $line' <&5
    cat <&5

    # For combination of groovyserver and groovyclient
    $SHOULD_SHOW_VERSION_LATER && version
    $SHOULD_SHOW_USAGE_LATER && (echo ; usage)

    # fixed at 0
    return 0
}

# ------------------------------------------
# Setup global variables only for here
# ------------------------------------------

SHOULD_SHOW_USAGE_LATER=false
SHOULD_SHOW_VERSION_LATER=false
unset AUTHTOKEN
SERVER_ARGS=()
SERVER_OPTIONS=()
DO_KILL=false
DO_RESTART=false
ENV_INCLUDES=()
ENV_EXCLUDES=()

#-------------------------------------------
# Main
#-------------------------------------------

# Pre-processing
check_environment

# Parse arguments
while [ $# -gt 0 ]; do
    case $1 in
        -Chelp | -Ch)
            usage
            exit 0
            ;;
        --help | -h)
            SERVER_ARGS+=("$1")
            SHOULD_SHOW_USAGE_LATER=true
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
        -Ckill-server | -Ck)
            SERVER_OPTIONS+=("-k")
            DO_KILL=true
            shift
            ;;
        -Crestart-server | -Cr)
            SERVER_OPTIONS+=("-r")
            DO_RESTART=true
            shift
            ;;
        -Cquiet | -Cq)
            SERVER_OPTIONS+=("-q")
            QUIET=true
            shift
            ;;
        -Cenv)
            shift
            ENV_INCLUDES+=("$1")
            shift
            ;;
        -Cenv-all)
            ENV_ALL=true
            shift
            ;;
        -Cenv-exclude)
            shift
            ENV_EXCLUDES+=("$1")
            shift
            ;;
        -Cversion | -Cv)
            version
            exit 0
            ;;
        --version | -v*)
            SERVER_ARGS+=("$1")
            SHOULD_SHOW_VERSION_LATER=true
            shift
            ;;
        *)
            SERVER_ARGS+=("$1")
            shift
    esac
done

# Display additionally client's usage at the end of session when no arguments for server
if [ "${#SERVER_ARGS[@]}" -eq 0 ]; then
    SHOULD_SHOW_USAGE_LATER=true
fi

# Start or stop server when specified
start_server

# Request to server
start_session

