#!/bin/bash
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

# Specifying platform (os, arch)
case `uname` in
    Darwin)
        os=darwin
        ;;
    CYGWIN*)
        os=windows
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
platform="${os}_${arch}"

# Confirming a platform directory existence
from_dir="$DIRNAME/../platforms/$platform"
if [ ! -d "$from_dir" ]; then
    echo "ERROR: your platform not supported: $platform" >&2
    echo "Sorry, please build by yourself. See http://kobo.github.io/groovyserv/howtobuild.html" >&2
    exit 1
fi

# Copying commands
bin_dir="$DIRNAME"
cp "$from_dir/"* "$bin_dir"
chmod +x "$bin_dir/"*

echo "Setup completed successfully for $platform"
