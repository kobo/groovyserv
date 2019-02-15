How to Install
==============

## For each environment

* For all Un*x platform
    - [SDKMAN](#sdk)
* Only for Mac OS X
    - [Homebrew](#homebrew)
* Only for Linux
    - [An unofficial RPM package](#rpm)
* Only for Windows
    - [Groovy's Windows-Installer (including GroovyServ)](http://groovy-lang.org/download.html)
* For Anywhere:
    - [binary package (bin.zip)](#binary)


## Install by [SDKMAN](https://sdkman.io) {#sdk}

You can install the latest GroovyServ:

```sh
$ sdk install groovyserv
Downloading: groovyserv 1.2.0

In progress...

###################### 100.0%

Installing: groovyserv 1.2.0
Done installing!

Do you want groovyserv 1.2.0 to be set as default? (Y/n):

Setting groovyserv 1.2.0 as default.
```

Then, you can use just `groovyclient` and a `groovyserver` just after the installation.
For example:

```sh
$ groovyclient -v
Setup completed successfully for darwin_amd64.
It's required only just after installation. Please run the same command once again.
```

When you run the same command again, it works well expectedly:

```sh
$ groovyclient -v
Groovy Version: <GROOVY_VERSION> JVM: <JAVA_VERSION> Vendor: Oracle Corporation OS: Mac OS X
GroovyServ Version: Server: <VERSION>
GroovyServ Version: Client: <VERSION>
```


The binary package includes binary files of user commands only for Mac OS X, Linux(amd64/i386) and Windows.
In case that you want to install to other environments, you can easily build the commands if there is `go` command.
See [How to build](howtobuild.md).


## Install by [Homebrew](http://mxcl.github.com/homebrew/) {#homebrew}

You can install the latest GroovyServ:

```sh
$ brew install groovyserv
```


## Install from a binary package {#binary}

Download and expand a GroovyServ binary package from [Download page](download.md).

```sh
$ cd /tmp
$ unzip groovyserv-<VERSION>-bin.zip
$ groovyserv-<VERSION>-bin/bin/setup.sh
```

You should add the expanded `bin` directory to `PATH` environment variable.
For example in bash/bourne shell:

```sh
export PATH=/tmp/groovyserv-<VERSION>/bin:$PATH
```

The binary package includes binary files of user commands only for Mac OS X, Linux(amd64/i386) and Windows.
In case that you want to install to other environments, you can easily build the commands if there is `go` command.
See [How to build](howtobuild.md).


## Install from a RPM package {#rpm}

Currently we don't provide a RPM package officially.
But there is a contributed SPEC file which is required to build a RPM file.
So you can try to build it if you want.
See [Build RPM file](howtobuild.md#rpm).
