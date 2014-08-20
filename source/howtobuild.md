# How to Build

## Requirements

To build is required:

* [Java SDK 7+](http://www.oracle.com/technetwork/java/javase/downloads)
* [Go 1.3+](http://golang.org/doc/install)


> ###### INFO: For Windows users
> I think you can build on Cygwin in Windows.
> But if there are environment variables specialized for Cygwin (e.g. `/cygdrive/c/your/dir/path`), you will fail.
> So I recommend building on a DOS prompt or PowerShell, instead.

## Build a binary package dedicated to your environment

After downloading and expanding a GroovyServ's source package `groovyserv-<VERSION>-src.zip` to any directory, build it with Gradle as follows:

```
$ cd /tmp
$ unzip groovyserv-<VERSION>-src.zip
$ cd groovyserv-<VERSION>-src
$ ./gradlew clean distLocalBin
```

Then a ZIP file will be generated.

```
build/distributions/groovyserv-<VERSION>-bin-local.zip
```

This zip file is dedicated to your environment.
So you can use it immediately just after expanding it.


## Build a general binary package

After downloading and expanding a GroovyServ's source package `groovyserv-<VERSION>-src.zip` to any directory, build it with Gradle as follows:

```
$ cd /tmp
$ unzip groovyserv-<VERSION>-src.zip
$ cd groovyserv-<VERSION>-src
$ ./gradlew clean dist
```

Then some ZIP files will be generated.

```
build/distributions/groovyserv-<VERSION>-bin.zip
```

According to [Install from binary package](howtoinstall.md#binary), install it.


## Build a RPM package {#rpm}

If you want to install GroovyServ from a RPM package, you can build it by yourself.
The sample SPEC file which is required to build a RPM package was contributed by Oliver at first.
Then, Kazuhisha keeps update it.
Thank you, Oliver and Kazuhisha!
The samples of the SPEC file is in [contrib/rpm-spec](https://github.com/kobo/groovyserv/tree/master/contrib/rpm).
Well, I don't know how to build a RPM package, so I cannot support about it.
Sorry in advance ;-)


## Testing

If you want to run tests (including unit tests and integration tests), run Gradle with following arguments:

```
$ export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
$ ./gradlew test
```
