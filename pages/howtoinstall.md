# How to Install

## For each environment

* For Anywhere:
    - [binary package (bin.zip)](#howtoinstall-binary)
* For all UNIX platform
    - [GVM](#howtoinstall-gvm)
* Only for Mac OS X
    - [Homebrew](#howtoinstall-homebrew)
* Only for Linux
    - [An unofficial RPM package](#howtoinstall-rpm)
* Only for Windows
    - Groovy's Windows-Installer (including GroovyServ): http://groovy.codehaus.org/Download


<span id="howtoinstall-binary"></span>
## Install from binary package

Download and expand GroovyServ distribution package from [Download page](./download.html), e.g. `groovyserv-0.13-macosx-bin.zip` to any directory:

```
$ mkdir ~/opt
$ cd ~/opt
$ unzip groovyserv-0.13-macosx-x86_64-bin.zip
```

And add the bin directory to `PATH` environment variable.
For example in bash/bourne shell::

```
export PATH=~/opt/groovyserv-0.13/bin:$PATH
```

That's all for preparing.
When you invoke groovyclient, groovyserver automatically starts in background.
At first, you might have to wait for a few seconds to startup:

```
$ groovyclient -v
Invoking server: '/opt/groovyserv-0.13/bin/groovyserver' -p 1961
...
groovyserver 24008(1961) is successfully started
Groovy Version: 2.1.5 JVM: 1.7.0_25 Vendor: Oracle Corporation OS: Mac OS X
GroovyServ Version: Server: 0.13
GroovyServ Version: Client: 0.13 (.c)
```

If the binary downloaded doesn't work, try to build it from source code, according to [Build from source code](./howtobuild.html).


<span id="howtoinstall-gvm"></span>
## Install by GVM

To install:

```
gvm install groovyserv
```

A `groovyclient` command installed by GVM is a limited shell version.
If you have `make` and `gcc`, you can easily replace it with a native client built by yourself.
See [Build native groovyclient on GVM](./howtobuild.html#gvm-nativeclient).

See also: http://gvmtool.net/


<span id="howtoinstall-homebrew"></span>
## Install by Homebrew

To install:

```
$ brew install groovyserv
```

See also: http://mxcl.github.com/homebrew/


<span id="howtoinstall-rpm"></span>
## Install from RPM package

Currently we don't produce a RPM package officially. But there is the contributed SPEC file which is required to build a RPM file.
So you can try to build it by yourself.
See [Build RPM file](./howtobuild.html#rpm).
