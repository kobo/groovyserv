# How to Install

## For each environment

* For all Un*x platform
    - [GVM](#gvm)
* Only for Mac OS X
    - [Homebrew](#homebrew)
* Only for Linux
    - [An unofficial RPM package](#rpm)
* Only for Windows
    - [Groovy's Windows-Installer (including GroovyServ)](http://groovy-lang.org/download.html)
* For Anywhere:
    - [binary package (bin.zip)](#binary)


<span id="gvm"></span>
## Install by [GVM](http://gvmtool.net/)

You can install the latest GroovyServ:

```
$ gvm install groovyserv
```

User commands aren't available just after the installation by GVM.
So you have to run a setup script:

```
$ ~/.gvm/groovyserv/current/bin/setup.sh
```

The binary package includes binary files only for Mac OS X, Linux(amd64/i386) and Windows.
But, no problem.
In case that you want to install to other environments, it will be automatically built if only there is `go` command.


<span id="homebrew"></span>
## Install by [Homebrew](http://mxcl.github.com/homebrew/)

You can install the latest GroovyServ:

```
$ brew install groovyserv
```


<span id="binary"></span>
## Install from a binary package

Download and expand a GroovyServ binary package from [Download page](./download.html).

```
$ cd /tmp
$ unzip groovyserv-<VERSION>-bin.zip
$ groovyserv-<VERSION>-bin/bin/setup.sh
```

You should add the expanded `bin` directory to `PATH` environment variable.
For example in bash/bourne shell:

```
export PATH=/tmp/groovyserv-<VERSION>/bin:$PATH
```

The binary package includes binary files only for Mac OS X, Linux(amd64/i386) and Windows.
But, no problem.
In case that you want to install to other environments, it will be automatically built if only there is `go` command.


<span id="rpm"></span>
## Install from a RPM package

Currently we don't provide a RPM package officially.
But there is a contributed SPEC file which is required to build a RPM file.
So you can try to build it if you want.
See [Build RPM file](./howtobuild.html#rpm).
