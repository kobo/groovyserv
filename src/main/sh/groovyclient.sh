#!/bin/bash
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

#-------------------------------------------
# OS specific support
#-------------------------------------------

OS_CYGWIN=false
OS_MSYS=false
OS_DARWIN=false
case "`uname`" in
  CYGWIN* )
    OS_CYGWIN=true
    ;;
  Darwin* )
    OS_DARWIN=true
    ;;
  MINGW* )
    OS_MSYS=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
# When they are used by Groovy, Groovy's script will convert them appropriately.
if $OS_CYGWIN; then
    # TODO Original Groovy's shell scirpt uses only HOME instead of USERPROFILE.
    # In GroovyServ, let it be in order to unify the work directory for both cygwin and BAT.
    HOME=`cygpath --unix --ignore "$USERPROFILE"`
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
    echo "*** NOTE ***"
    echo "  This client scirpt is LIMITED EDITION. So following features are unavailable:"
    echo "    * Transparent server invocation (only starting server is available)"
    echo "    * Signal handling on client side (Ctrl+C)"
    echo "    * System.in from client"
    echo "    * Distinguishable stdout from stderr on client (all responses to stdout)"
    echo "    * Status code from server ($?)"
}

version() {
    echo "GroovyServ Version: Client: @GROOVYSERV_VERSION@ (.sh) [Limited Edition]"
}

error_log() {
    local message="$1"
    /bin/echo "ERROR: $message" 1>&2
}

info_log() {
    local message="$1"
    if [ ! $QUIET ]; then
        /bin/echo "$message" 1>&2
    fi
}

debug_log() {
    local message="$1"
    if [ $DEBUG ]; then
        /bin/echo "DEBUG: $message" 1>&2
    fi
}

die() {
    local message="$*"
    error_log "$message"
    usage
    exit 1
}

resolve_symlink() {
    local target=$1

    # if target is symbolic link
    if [ -L $target ]; then
        local ORIGINAL_FILEPATH=`readlink $target`

        # if original is specified as absolute path
        if [ $(echo $ORIGINAL_FILEPATH | cut -c 1) = "/" ]; then
            echo "$ORIGINAL_FILEPATH"
        else
            echo "$(dirname $target)/$ORIGINAL_FILEPATH"
        fi
    else
        echo "$target"
    fi
}

expand_path() {
    local target=$1
    if [ -d "$target" ]; then
        echo $(cd $target && pwd -P)
    elif [ -f "$target" ]; then
        local TARGET_RESOLVED=$(resolve_symlink $target)
        local FILENAME=$(basename $TARGET_RESOLVED)
        local DIR_EXPANDED="$(expand_path $(dirname $TARGET_RESOLVED))"
        echo "$DIR_EXPANDED/$FILENAME"
    else
        echo "$target"
    fi
}

check_port() {
    local port=$1
    netstat -an | grep "[.:]${port} .* LISTEN" >/dev/null 2>&1
}

# ------------------------------------------
# GroovyServ's work directory
# ------------------------------------------

GROOVYSERV_WORK_DIR="$HOME/.groovy/groovyserv"
if [ ! -d "$GROOVYSERV_WORK_DIR" ]; then
    mkdir -p "$GROOVYSERV_WORK_DIR"
fi
debug_log "GroovyServ work directory: $GROOVYSERV_WORK_DIR"

#-------------------------------------------
# Port and PID and AuthToken
#-------------------------------------------

GROOVYSERVER_HOST=localhost
GROOVYSERVER_PORT=${GROOVYSERVER_PORT:-1961}
GROOVYSERV_AUTHTOKEN_FILE="$GROOVYSERV_WORK_DIR/authtoken-$GROOVYSERVER_PORT"
GROOVYSERVER_CMD=$(expand_path $(dirname $0)/groovyserver)

#-------------------------------------------
# Parse arguments
#-------------------------------------------

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

#-------------------------------------------
# Core functions
#-------------------------------------------

start_server() {
    # To try only for localhost
    [ "$GROOVYSERVER_HOST" != "localhost" ] && return

    if ! check_port $GROOVYSERVER_PORT; then
        info_log "Invoking server: '$GROOVYSERVER_CMD' -p $GROOVYSERVER_PORT"
        $GROOVYSERVER_CMD
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
    if [ -n "$ENV_ALL" ]; then
        local env_names=$(env | awk -F= '{ print $1 }')
    elif [ ${#ENV_INCLUDES[@]} -gt 0 ]; then
        local env_names=${ENV_INCLUDES[@]}
    fi
    if [ -n "$env_names" ]; then
        for env_name in $env_names; do
            local excluded=false
            for excluded_key in "${ENV_EXCLUDES[@]}"; do
                if [ "$env_name" == "$excluded_key" ]; then
                    excluded=true
                fi
            done
            if ! $excluded; then
                echo "Env: $(env | grep -E "^$env_name=")"
            fi
        done
    fi

    # Authtoken
    echo "AuthToken: ${AUTHTOKEN:-$(cat $GROOVYSERV_AUTHTOKEN_FILE)}"

    # Arguments for groovy command
    for arg in "${SERVER_OPTIONS[@]}"; do
        echo "Arg: $(printf "%s" "$arg" | base64)"
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

start_server
start_session

