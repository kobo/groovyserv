.. _ref-howtobuild:

How to Build
============

Requirements
------------

To build is required::

  - JDK 7u25
  - make
  - gcc4
  - Cygwin/MinGW (only for Windows)


Build binary zip by Gradle
--------------------------

Download and expand GroovyServ source package `groovyserv-0.13-src.zip` to any directory::

    $ mkdir -p ~/opt/src
    $ cd ~/opt/src
    $ unzip groovyserv-0.13-src.zip

Build with Gradle as follows::

    $ cd ~/opt/src/groovyserv-0.13
    $ ./gradlew

Then some zip files will be generated.
According to :ref:`Install from binary package <ref-howtoinstall-binary>`, install the bin package::

    ~/opt/src/groovyserv-0.13/build/distributions/groovyserv-0.13-<OS>-<arch>-bin.zip

If some tests fail, please try again with specifying character encoding as follows::

    $ export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
    $ ./gradlew

or to skip all tests::

    $ ./gradlew clean dist

To build in Windows, you need gcc(v4) and MinGW.
You must install them before trying to build.
Then, execute the above commands on Cygwin.


Build only native groovyclient
------------------------------

There is the Makefile which can build `groovyclient` from C sources.
So you can easily build `groovyclient` binary file as follows::

    $ cd ~/opt/src/groovyserv-0.13
    $ make clean
    $ make

The binary file is created in `build` directory.
You can copy it whereever you want::

    ~/opt/src/groovyserv-0.13/build/natives/groovyclient      (for Linux / Mac OS X)
    ~/opt/src/groovyserv-0.13/build/natives/groovyclient.exe  (for Windows)


.. _ref-howtobuild-gvm-nativeclient:

Build native groovyclient on GVM
--------------------------------

`groovyclient` command installed by GVM is a limitted shell version.
If you have `make` and `gcc`, you can easily replace it with a native client built by yourself using `groovyserv-setup-nativeclient` command::

    $ groovyclient -v
    Groovy Version: xxxx
    GroovyServ Version: Server: 0.13
    GroovyServ Version: Client: 0.13 (.sh) [Limited Edition]

    $ groovyserv-setup-nativeclient
    ....
    Successfully installed: /xxxx/.gvm/groovyserv/xxxx/bin/groovyclient

    $ groovyclient -v
    Groovy Version: xxxx
    GroovyServ Version: Server: 0.13
    GroovyServ Version: Client: 0.13 (.c)


.. _ref-howtobuild-rpm:

Build RPM file
--------------

If you want to install from RPM package, you can build the RPM package by yourself.
The sample SPEC file which is required to build a RPM package was at first contributed by Oliver.
And Kazuhisha updated it. Thank you, Oliver and Kazuhisha!

The samples of SPEC file is in `contrib/rpm-spec <https://github.com/kobo/groovyserv/tree/master/contrib/rpm>`_.

Well, I don't know how to build a RPM package, so I may not be able to support about it.
Sorry in advance ;-)

