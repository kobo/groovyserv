.. _ref-howtobuild:

How to Build
============

Build binary zip by Gradle
--------------------------

Download and expand GroovyServ source package groovyserv-0.10-src.zip to any directory.
For example::

    $ mkdir -p ~/opt/src
    $ cd ~/opt/src
    $ unzip groovyserv-0.10-src.zip

Build with Gradle as follows::

    $ cd ~/opt/src/groovyserv-0.10
    $ ./gradlew

Then some zip files will be generated. According to :ref:`Install from binary package <ref-howtoinstall-binary>`, install the bin package::

    ~/opt/src/groovyserv-0.10/build/distributions/groovyserv-0.10-<OS>-<arch>-bin.zip

If some tests fail, please try again with specifying character encoding as follows::

    $ export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
    $ ./gradlew

or skip all tests::

    $ ./gradlew clean dist

To build in Windows, you need gcc-3 and MinGW. You must install them before trying to build.
Then, execute the above commands on Cygwin.


Build binary zip by Maven
-------------------------

Download and expand GroovyServ source package groovyserv-0.10-src.zip to any directory.
For example::

    $ mkdir -p ~/opt/src
    $ cd ~/opt/src
    $ unzip groovyserv-0.10-src.zip

Build with Maven as follows (recommended Maven3.x since v0.6)::

    $ cd ~/opt/src/groovyserv-0.10
    $ mvn clean verify

Then some zip files will be generated. According to :ref:`Install from binary package <ref-howtoinstall-binary>`, install the bin package::

    ~/opt/src/groovyserv-0.10/target/groovyserv-0.10-<OS>-<arch>-bin.zip

If some tests fail, please try again with specifying character encoding as follows::

    $ export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
    $ mvn clean verify

or skip integration tests::

    $ mvn clean package

or skip all tests::

    $ mvn -Dmaven.test.skip=true clean package

To build in Windows, you need gcc-3 and MinGW. You must install them before trying to build.
Then, execute the above commands on Cygwin.


Build only groovyclient
-----------------------

Since v0.8, there is the Makefile which can build a groovyclient from C sources.
You can easily build a groovyclient binary file as follows::

    $ cd ~/opt/src/groovyserv-0.10
    $ make clean
    $ make

The binary file is created in target directory. You can copy it where you want::

    ~/opt/src/groovyserv-0.10/target/groovyclient      (for Linux / Mac OS X)
    ~/opt/src/groovyserv-0.10/target/groovyclient.exe  (for Windows)


.. _ref-howtobuild-rpm:

Build RPM file
--------------

If you want to install from RPM package, you can build the RPM package by yourself. The sample SPEC file which is required to build a RPM package was at first contributed by Oliver. And Kazuhisha updated it. Thank you, Oliver and Kazuhisha!

The samples of SPEC file is in `contrib/rpm-spec <https://github.com/kobo/groovyserv/tree/master/contrib/rpm>`_.

Well, I don't know how to build a RPM package, so I may not be able to support about it. Sorry in advance ;-)

