.. _ref-changelog:
.. role:: alert
..
   Version 0.N (YYYY-MM-DD)
   ------------------------
   New Features:
   Improvements:
   Bug Fixes:
   Contributions:

==========
Change Log
==========

Version 0.13 (2013-07-26)
-------------------------
New Features:
    - [Client] `groovyclient(.sh)` supports server operations by `-Cr/-Ck` options
    - [Install] `groovyserv-setup-nativeclient` command is added to platform independent package to build a native `groovyclient` and replace `groovyclient(.sh)` shell script with it.

Improvements:
    - `groovyclient.rb` supports `-Chost` and `-Cauthtoken`
    - `GROOVYSERVER_HOST` environment variable is supported to specify server address from client
    - (Improved some checks when starting server)
    - (Spock-nized all tests)
    - (Improved build system)
    - (Thoroughly refactored shell scripts)

Bug Fixes:
    - Fixed 48: `groovyclient(.sh)` installed by GVM causes an error

        - https://github.com/kobo/groovyserv/issues/48

    - Fixed 49: `groovyclient` doesn't work with non-default ports

        - https://github.com/kobo/groovyserv/issues/49


Version 0.12 (2013-07-04)
-------------------------
New Features:
    - [Client] groovyclient.sh is added. In the platform-independent binary package, its name is just 'groovyclient.'
      It has some restrictions.  See :ref:`groovyclient's option in User Guide <ref-userguide-groovyclient-bash>`.

Improvements:
    - groovyclient.rb supports -Chost and -Cauthtoken

Bug Fixes:
    - Fixed 47: Command line parameters --allow-from and --authtoken do not work on Windows 7

        - https://github.com/kobo/groovyserv/issues/47


Version 0.11 (2013-02-01)
-------------------------
New Features:
    - [Client/Server] Supported fully remote access.

        - [Client] groovyclient can send a valid request to groovyserver on the other machine by using -Chost and -Cauthtoken.
        - [Server] you can provide an arbitrary authtoken when invoking groovyserver.

Improvements:
    - Renamed "cookie" to "authtoken".

Bug Fixes:
    - FIxed 44: caused a freeze when reading from stdin on Linux

        - https://github.com/kobo/groovyserv/issues/44

    - Fixed 45: cookie file permission is 644

        - https://github.com/kobo/groovyserv/issues/45

    - Fixed 46: standard out/err doesn't flush at ruby client when the content includes no line separator

        - https://github.com/kobo/groovyserv/issues/46


Version 0.10 (2012-03-30)
-------------------------
New Features:
    - [Client] -v option can also display the GroovyServ's version not only Groovy's version.
    - [Network] Supported --allow-from option at groovyserver(sh). If 'localhost' is resolved except to loopback address in your environment, you can use --allow-from option with groovyserver(sh). In future, I want to support that groovyclient could access a groovyserver at remote host in security. This is the first step.

Bug Fixes:
    - Fixed 39: Processing of Groovy cmd line args is inconsistent

        - https://github.com/kobo/groovyserv/issues/39

    - Fixed 40: "System.env[name]" cannot access environment variables at client side

        - https://github.com/kobo/groovyserv/issues/40

    - Fixed 41: ruby client cannot invoke groovyserver automatically in windows 7

        - https://github.com/kobo/groovyserv/issues/41

    - And other small fixes and improvements are included.


Version 0.9 (2011-08-04)
------------------------
Improvements:
    - [Env] Now you can use PWD environment variable. PWD is individually changed to the current directory for each invocation of user script.
    - [Build] You can build GroovyServ by Gradle. Maven's pom.xml is temporarily still there, but maybe it will be removed at next version.
    - [Performance] The overhead at sequential invocation of groovyclient was reduced. You needed at least one second as a interval of each sequential invocation at v0.8, but at v0.9 you might not be able to notice the overhead.
    - [Internal] To close connections and to terminate threads are improved. GroovyServ will probably behave more similarly to the regular Groovy than before.

Bug Fixes:
    - Fixed #33 : When a path of GROOVY_HOME was including a white space, the invocation of groovyserver was failed at v0.8

        - https://github.com/kobo/groovyserv/issues/33

    - Parsing options of Ruby client was wholly refactored. So, some bugs were fixed.

        - Fixed #34 : When invoking "groovyclient.rb -Cr -Cq", -Cq was ignored

            - https://github.com/kobo/groovyserv/issues/34

        - Fixed #35 : When transparently invoking groovyserver by groovyclient.rb, user script isn't invoked

            - https://github.com/kobo/groovyserv/issues/35

    - And other small fixes and improvements are included.

Contributions:
    - [Build] The spec file required to build a RPM package is there. See: :ref:`Build RPM file <ref-howtobuild-rpm>` (Thanks, Oliver and Kazuhisha)


Version 0.8 (2011-06-16)
------------------------
Improvements:
    - [Windows/Cygwin] groovyserver.bat and groovyserver(sh) were wholly improved:

        - When GROOVY_HOME or GROOVYSERV_HOME isn't set, it's automatically detected.
        - JAVA_HOME, GROOVY_HOME, GROOVYSERV_HOME and CLASSPATH are converted to the appropriate path format if necessary.
        - So, you can also use groovyclient.exe to start a groovyserver on Cygwin even if you've set GROOVY_HOME or GROOVYSERV_HOME as an UNIX path format. (It had failed at v0.7.)
        - Now there isn't the dirty hack using ping to sleep 1 sec.

    - [Log File] The dump data in log file is more easy to read. Until v0.7, many printable characters except alphabet and number were printed as "?", but now they become being printed as original character.

    - [Build] Makefile is added. You can more easily build a binary of groovyclient with "make" command if the downloaded binary doesn't work.

Bug Fixes:
    - Fixed #28: malloc/free error occurred with a --classpath option

        - https://github.com/kobo/groovyserv/issues/28

    - Fixed #30: cannot invoke a script when transparently restarting server from client

        - https://github.com/kobo/groovyserv/issues/30

    - Fixed #31: "dgroovyclient -h" on Windows shows unexpected behaviors

        - https://github.com/kobo/groovyserv/issues/31

    - Fixed #32: using groovyclient.rb, user.dir is based on cygwin path format on Cygwin

        - https://github.com/kobo/groovyserv/issues/32

Version 0.7 (2011-04-27)
------------------------
Improvements:
    - You can see CLASSPATH information of a groovyserver at start-up messages (Mac OS X and Linux only).
    - Experimentally, the "-server" of JVM option was added to a groovyserver.
      Hotspot may improve the performance of an execution of a script.

Bug Fixes:
    - Fixed #14: groovyclient -Cenv -e "println System.env.HI" don't work expectedly

        - https://github.com/kobo/groovyserv/issues/14

    - Fixed #23: buffer over flow of encoding arguments as base64 in groovyclient of C

        - https://github.com/kobo/groovyserv/issues/23

    - Fixed #24: groovyserver process wrongly exits on failing decoding of base64

        - https://github.com/kobo/groovyserv/issues/24

    - Fixed #25: native groovyclient of v0.6 cannot be invoked with Windows 7 64bits

        - https://github.com/kobo/groovyserv/issues/25

    - Fixed #26: groovyserver(sh) doesn't take over CLASSPATH environment variable

        - https://github.com/kobo/groovyserv/issues/26

    - Fixed #27: cannot access original environment variables of groovyserver

        - https://github.com/kobo/groovyserv/issues/27


Version 0.6 (2011-03-18)
------------------------
Improvements:
    - Supported for a multiline command argument. You can use an
      argument including newlines, by quoting the string with
      tripple-quotations appropriately.
    - Changed a log file name to include a port number as suffix.
      So, all work files (e.g. log, cookie and pid) were fully
      separated for each ports. Multiple instances of GroovyServ
      can be run simultaneously on separate ports.
    - Classpath never conflict. The user-specified classpath tears
      down at the end of each script invocation. (At the previous
      versions, a classpath could be added but couldn't be removed
      after the invocation).
    - Using a positive number as exit status code which is defined
      by GroovyServ as a constant. If your script depends on the
      return code from GroovyServ, you might need fix it.
    - Maven3.x support.

Bug Fixes:
    - Fixed #15: Document bug about groovyserv bash script availability

        - https://github.com/kobo/groovyserv/issues/closed#issue/15

    - Fixed #16: CWD is accidentally ovewritten by another session.

        - https://github.com/kobo/groovyserv/issues/closed#issue/16

    - Fixed #17, #18: Wrong file privileges in \*nix distribution.

        - https://github.com/kobo/groovyserv/issues/closed#issue/17
        - https://github.com/kobo/groovyserv/issues/closed#issue/18

    - Fixed #19: GROOVYSERV_HOME resolution fails when binaries are symbolic links when using SH.

        - https://github.com/kobo/groovyserv/issues/closed#issue/19

    - Fixed #21: wrong check for -Cenv, -Cenv-exclude in groovyclient.rb.

        - https://github.com/kobo/groovyserv/issues/closed#issue/21

    - Fixed #22: the first environment variable is sometimes not passed to server.

        - https://github.com/kobo/groovyserv/issues/closed#issue/22


Version 0.5 (2010-12-22)
------------------------

New Features:
    - Now groovyclient can take -C prefix options which are interpreted
      on groovyclient itself (don't pass to groovy command).
    - '-Cr/-Ck' client options restart/stop groovyserver process(Mac OS X and
      Linux only). By using these options you can control groovyserver
      through groovyclient.
    - With '-Cenv' option of groovyclient, we can pass the environment
      variables which name matches with the specified pattern to
      groovyserver. The values of matched variables on the client process
      are sent to the server process, and the values of same name
      environment variable on the server are set to or overwitten by the
      passed values. This feature is especially useful for IDEs' which
      uses environment variables to pass configuration information with
      invoking external command which is written Groovy. Textmate is one
      of those IDEs reportedly.
    - Option '-Cenv-all' makes to pass all environment variables of the client
      process to groovyserver. And option '-Cenv-exclude' excludes variables
      which name matches with the specified pattern.
    - You can specify port number easily by using -Cp/-Cport options and
      handle multiple groovyserver instance distinguished by the port number.

Improvements:
    - Groovyserver's starting messages are now emit to stderr, so use of
      pipe or redirection is more useful with Groovy scripting. Moreover
      -Cq/-Cqiuet options suppresses starting message of groovyserver.
    - Groovyserver now emit informative messages about GroovyServ's
      installed directory and which groovy command are used. This is
      useful for trouble shooting.
    - [Only Linux/Mac OS X]Now GROOVY_HOME environment variable become
      optional. Supported some ways to find a groovy command in the
      following order: (1)from PATH environment variable. (2)as
      GROOVY_HOME/bin/groovy. If not exists, a intent revealing message
      is emitted to the console.

Bug fixes:
    - Groovyclient can't invoke groovyserver when GroovyServ is installed
      on a directory which name includes white spaces(e.g. 'C:/Program
      Files/...).
    - On Linux, if the GroovyServ installed to the directory with
      symbolic link, it didn't work. (because of Linux's which command
      don't support -s option so we changed the implementation to do it
      without -s.)
    - In Mac OS X, progress showing mark displayed '-n .' instead of '...'.


Version 0.4 (2010-08-06)
------------------------

New Features:
    - groovyserver.bat shows a window which can be used to stop the server in Windows.

Improvements:
    - Supported -p, -n options of groovyclient (See help of groovy command).
    - Filters written in Groovy works well.
    - Using a environment variable USERPROFILE instead of HOME in Windows.
    - Improved support of invoking groovyserver on Cygwin.
    - Improved a process of invoking groovyserver.
    - Packaged not-compiled Groovy scripts into jar file in order not to depend on a particular JDK version.
    - Printing help message when groovyclient is run without options.
    - Appended date and time in debug log.
    - And you can also execute the following command::

       $ ls | groovyclient -e "System.in.eachLine{ println it }"

Bug fixes:
    - Cannot print nothing after an error about pipe occurred once.
    - Segmentation fault occurs with too long arguments.
    - Some bugs makes segmentation faults around communication handling is fixed.
    - Fix for ignored CLASSPATH environment variable in groovyclient.rb


Version 0.3 (2010-07-14)
------------------------

Improvements:
    - Refactored groovyclient.c a little.
    - README has URL of the site of GitHub (and README.ja was deleted).
    - Upgraded Groovy 1.7.2 -> 1.7.3.
    - Upgraded gmaven 1.2 -> 1.3-SNAPSHOT (because GMAVEN-13 was fixed).
    - All tests result green in Windows environment.
    - Tweaked handling of character encoding in a build sequence. you can use either of the following:

        - default encoding (without -Dfile.encoding in JAVA_OPTS, _JAVA_OPTIONS)
        - global encoding with -Dfile.encoding in _JAVA_OPTIONS (recommend: UTF-8)

Bug Fixes:
    - Fixed that groovyserver.bat doesn't start up through groovyclient in Windows.
    - Fixed that loop count is wrong while starting server in groovyclient.c.


Version 0.2 (2010-06-30)
------------------------

New Features:
    - Independence from cygwin.dll on Windows environment.  Now it can be compiled with MinGW.
    - Added batch file version groovyserver startup script (for uses on Windows who don't want to install Cygwin).

Improvements:
    - Wholly refactored implementation.
    - Added unit tests and integration tests (some of integration tests are a little fragile still now).
    - Output more informative log with -v option.
    - All log output to ./groovy/groovyserv/groovyserver.log.


Version 0.1 (2010-03-09)
------------------------

First release.

