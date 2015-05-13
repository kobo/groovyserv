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

# Resolving a path
script_path="$0"
while [ -h "$script_path" ] ; do
    ls=`ls -ld "$script_path"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        script_path="$link"
    else
        script_path=`dirname "$script_path"`/"$link"
    fi
done
current_dir=`dirname "$script_path"`

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
    *64)
        arch=amd64
        ;;
    *)
        arch=386
        ;;
esac
if [ $os = "windows" ]; then
    platform="windows_386"
else
    platform="${os}_${arch}"
fi

# Confirming a platform directory existence
from_dir="$current_dir/../platforms/$platform"
if [ ! -d "$from_dir" ]; then
    echo "ERROR: your platform not supported: $platform" >&2
    echo "Sorry, please build by yourself. See http://kobo.github.io/groovyserv/howtobuild.html" >&2
    exit 1
fi

# Removing dummy scripts
bin_dir="$current_dir"
rm "$bin_dir/"{groovyserver{,.bat},groovyclient{,.bat},setup.{sh,bat}}

# Copying commands
cp "$from_dir/"* "$bin_dir"
chmod +x "$bin_dir/"*

# End messages
echo "Setup completed successfully for $platform."
if [ `basename $script_path` != "setup.sh" ]; then
    echo "It's required only just after installation. Please run the same command once again."
fi
