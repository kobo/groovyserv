.. _ref-readme:

README
======

Introduction
------------

GroovyServ makes startup time quicker, by pre-invoking groovy as a TCP/IP
server. If you know gnuserv(gnudoit)/emacsserver/emacsclient, this is like that.

In the case of scripting in dynamic-type languages, quick response about
invocation is very important. Try-and-run cycles is repeated frequently
than static-type languages, so sometimes 2 seconds or even a second might
be intolerable.

GroovyServ reduces the startup time of JVM and Groovy runtime
significantly. In most situations it is 10 to 20 times faster than
regular Groovy. The following times are averages of 3 times which
measured invocation in Groovy 1.7 on Windows XP (Core(TM) 2 Duo 2GHz).

    ==================  ===========
    Binary              Result(sec)
    ==================  ===========
    Groovy(non native)  2.32
    Groovy(native exe)  0.90
    GroovyServ          0.10
    ==================  ===========

Requirements
------------

GroovyServ is developed for following environment/OS. Please report if it
runs on the others.

  - Windows XP + Cygwin 1.7.x
  - Windows XP without Cygwin
  - Windows 7(64bit) without Cygwin
  - Mac OS X 10.5/6 (Intel Mac)
  - Ubuntu Linux 9.10

Version of JDK is following:

  - JDK 6u24 or later (Mac OS X)
  - JDK 6u21 or later (Windows, Linux)

Version of Groovy is following:

  - Groovy 1.7 or later

Language
--------

The Server is written in Java and Groovy with JNA(Java Native Access).
There are two types of clients, one is written in C and another is
written in Ruby.

Security
--------

GroovyServ has a possibility to run any groovy script from client.
So server-client connection is limited to the connection from the same
machine (localhost). And the connection is authenticated by simple
cookie mechanism.
The cookie file is stored at ~/.groovy/groovyserv/cookie-<port>
and the file mode set to 0400. But in Windows environment, it is not
effective. So appropriately protect access to the file in Windows if
needed.

Install from binary package
---------------------------

Download and expand GroovyServ distribution package, e.g.
groovyserv-0.8-macosx-bin.zip to any directory::

  $ mkdir ~/opt
  $ cd ~/opt
  $ unzip groovyserv-0.8-macosx-bin.zip

And add bin directory to PATH environment variable.
For example in bash/bourne shell::

  export PATH=~/opt/groovyserv-0.8/bin:$PATH

That's all for preparing. When you invoke groovyclient, groovyserver
automatically starts in background. At first, you might have to wait
for a few seconds to startup::

  $ groovyclient -v
  Invoking server: '/xxx/groovyserv-0.8/bin/groovyserver' -p 1961 
  Groovy home directory: (none)
  Groovy command path: /usr/local/bin/groovy (found at PATH)
  GroovyServ home directory: /xxx/groovyserv-0.8
  GroovyServ work directory: /Users/ynak/.groovy/groovyserv
  Original classpath: (none)
  GroovyServ default classpath: /xxx/groovyserv-0.8/lib/*
  Starting...
  groovyserver 75808(1961) is successfully started
  Groovy Version: 1.8.0 JVM: 1.6.0_24

.. _ref-readme-build:

Build from source code
----------------------

Download and expand GroovyServ source package groovyserv-0.8-src.zip
to any directory. For example::

  $ mkdir -p ~/opt/src
  $ cd ~/opt/src
  $ unzip groovyserv-0.8-src.zip

Build with Maven as follows (recommended Maven3.x since v0.6)::

  $ cd ~/opt/src/groovyserv-0.8/
  $ mvn clean verify

Then some zip files will be generated. According to "Install from
binary package", install the bin package::

  ~/opt/src/groovyserv-0.8/target/groovyserv-0.8-<OS>-<arch>-bin.zip

If some tests fail, you can try as follows::

  with UTF-8 encoding::

    $ export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
    $ mvn clean verify

  skip integration tests::

    $ mvn clean package

  skip all tests::

    $ mvn -Dmaven.test.skip=true clean package

To build it in Windows, you need gcc-3 and MinGW (recommended on Cygwin).
You must install them before trying to build.

.. _ref-readme-env:

Environment variables
---------------------

GroovyServ uses the following environment variables in runtime.

  HOME (only on Linux or Mac OS X)
    It's used to decide ~/.groovy/groovyserv directory path which is
    used for logging, storing a cookie and a PID. It's set by default
    on unix-like OS.

  USERPROFILE (only on Windows)
    It's used to decide ~\.groovy\groovyserv directory path which is
    used for logging, storing a cookie and a PID. It's set by default
    on Windows. If server process is invoked by groovyserver.bat, PID
    file doesn't exist.

  JAVA_HOME
    It's used by Groovy. Generally, it has been already set through
    installing Groovy.

  GROOVY_HOME (on Linux or Mac OS X: optional, on Windows: required)
    It's used to specify groovy command path. On Linux or Mac OS X,
    if you've set groovy command into PATH environment variable,
    you don't need it.

  GROOVYSERVER_PORT (optional)
    It's used to specify the port number for server or client.
    Alternately, you can specify the port as a command option.

  CLASSPATH (optional)
    CLASSPATH environment variable on where groovyserver starts up
    composes the environment variable of the groovyserver process,
    with Jars of GroovyServ. It's used as "default classpath" and
    affects invocations of every script.

    CLASSPATH environment variable on where groovyclient is invoked
    is transferred to the groovyserver and is dynamically set to
    the compiler's configuration of the script. (CLASSPATH environment
    variable of groovyserver is never modified.) The temporary classpath
    doesn't affect the next script invocation because it's reset on the
    tear-down phase of each script invocation.  When searching a class,
    groovyserver's CLASSPATH environment variable is used priorly.
    The above behavior is quite same as groovyclient's -cp option.

How to use
----------

You can use "groovyclient" command instead of "groovy" command.
If the server is not running yet, it automatically starts.

You can also invoke the server explicitly::

  $ groovyserver

With -v option, the verbose messages are written into a log file.
it's useful to analyze a problem about invoking server::

  $ groovyserver -v

About groovyserver command options, refer to "groovyserver's option"
section.

Restriction/Differences
-----------------------

* You can't concurrently use different current directory on a server.
  It also meets conditions if you invoke groovyclient simultaneously
  from two or more consoles. But if the running periods of each
  groovyclient are not overlapping, it can run without exception.

  If needed, you can simultaneously run multiple groovyservers with
  different ports.

* A static variable is shared among Groovy program invocations.
  For instance, the system properties are shared::

    $ groovyclient -e "System.setProperty('a','abc')"
    $ groovyclient -e "println System.getProperty('a')"
    abc

  However, System.out, System.in and System.err are rightly used
  which are individually prepared for each invocation.

* Normally, environment variables of when groovyserver was invoked
  are used instead of those of groovyclient side. But if you specify
  -Cenv/-Cenv-all option, you can reflect the values of environment
  variables of client to the server.

  Only the CLASSPATH environment variable, however, whichever with or
  without those options, is always reflected to the server. The values
  are cleared at the end of each client invocation. It doesn't affect
  to next invocation.

groovyclient's option
---------------------

groovyclient's options start with "-C". Those options are analyzed
and consumed by groovyclient, and aren't passed to groovy command::

  -Ch,-Chelp               show this usage
  -Cp,-Cport <port>        specify the port to connect to groovyserver
  -Ck,-Ckill-server        kill the running groovyserver
  -Cr,-Crestart-server     restart the running groovyserver
  -Cq,-Cquiet              suppress statring messages
  -Cenv <substr>           pass environment variables of which a name
                           includes specified substr
  -Cenv-all                pass all environment variables
  -Cenv-exclude <substr>   don't pass environment variables of which
                           a name includes specified substr

groovyserver's option
---------------------

groovyserver's options are as follows::

  -v         verbose output to the log file
  -q         suppress starting messages
  -k         kill the running groovyserver (groovyserver.bat cannot use this)
  -r         restart the running groovyserver (groovyserver.bat cannot use this)
  -p <port>  specify the port to listen

Start and stop groovyserver
---------------------------

There are two ways to invoke groovyserver; the one is, called "explicit
server invocation", the way of using "groovyserver" or "groovyserver.bat".
The another is, called "transparent server invocation",the way of just
using groovyclient. If groovyserver hasn't run yet, groovyclient
automatically invokes groovyserver at the background.

The commands for explicit server invocation are:

 - groovyserver      (for Mac OS X, Linux, Windows with Cygwin)
 - groovyserver.bat  (for Windows without Cygwin)

Following table shows the availability of those commands: (OK: Available, N/A: Not available)

    =================  =================  ==================  ===============
    Script             Windows w/ Cygwin  Windows w/o Cygwin  Mac OS X, Linux
    =================  =================  ==================  ===============
    groovyserver       OK                 N/A                 OK
    groovyserver.bat   OK                 OK                  N/A
    =================  =================  ==================  ===============

groovyserver.bat doesn't support -r and -k options for technical
reasons. So, on the command line, You can neither stop nor restart
the groovyserver started by groovyserver.bat. Instead, a minimized
window is shown when groovyserver is started by groovyserver.bat.
You can stop the groovyserver by closing the window. As a result,
then you can restart groovyserver by invoking groovyclient as
transparent server invocation.

On Cygwin, groovyclient internally uses groovyserver.bat for
transparent server invocation. Therefore, the behavior on Cygwin
is as follows:

- In the case of groovyserver explicitly invoked by groovyserver shell
  script, you can stop or restart the server by invoking groovyserver
  shell script with -k or -r options.

- In the case of groovyserver explicitly invoked by groovyserver.bat
  (bat file), you can stop the server by closing the window of the
  groovyserver.

- In the case of groovyserver transparently invoked by groovyclient.exe,
  you can stop the server by closing the window of the groovyserver.

It seems be confusing enough. So, we are considering to support -r and
-k options of groovyserver.bat.

In transparent server invocation, you cannot supply options(e.g. -v)
for groovyserver or groovyserver.bat which is invoked internally by
groovyclient. If you need, explicitly invoke groovyserver with options.

Propagation of environment variable
-----------------------------------

With -Cenv option of groovyclient, you can pass environment variables
of which a name includes the specified substring to groovyserver. The
values of those variables on the client process are sent to the server
process, and the values of same environment variables on the server are
set to or overwritten by the passed values. This feature is especially
useful for tools (e.g. IDE, TextMate) which invoke an external command
written by Groovy, and which uses environment variables to pass
parameters to the command.

When you specify the option -Cenv-all, all environment variables of the
groovyclient process are sent to the groovyserver. Additionally with the
option -Cenv-exclude, the variables of which a name includes specified
substring are excluded.

If you specify option::

  -Cenv SUBSTRING

the set of environment variables sent to the server are determined
by the following pseudo code::

  allEnvironmentVariables.entrySet().findAll {
    it.name.contains("SUBSTRING")
  }

Consider the combination of Cenv, -Cenv-all and -Cenv-exclude, like::

  -Cenv SUBSTRING
  -Cenv-all
  -Cenv-exclude EXCLUDE_SUBSTRING

The result of the following pseudo code are sent to the groovyserver::

  allEnvironmentVariables.entrySet().findAll {
    if (isSpecifiedEnvAll || it.name.contains("SUBSTRING")) {
      if (!it.name.contains("EXCLUDE_SUBSTRING")) {
        return true
      }
    }
    return false
  }

Note that the environment variables which is set to the groovyserver
remain after the groovyclient terminates. And modifying an environment
variable on a server are not thread-safe. So when multiple groovyclient
instances are invoked simultaneously, a variable which one of them needs
might be overwritten by another groovyclient subsequently invoked.

Port number
-----------

As a default, TCP port number which is used for communication between a
groovyserver and a groovyclient is 1961. To change a port number used by
a groovyserver, you can use GROOVYSERVER_PORT environment variable or -p
option. The -p option is used more prior than GROOVYSERVER_PORT environment
variable::

  $ export GROOVYSERVER_PORT=1963
  $ groovyserver

or::

  $ groovyserver -p 1963

On the other hand, for a groovyclient, you can use -Cp option instead
of -p which is used by Groovy and GROOVYSERVER_PORT environment variable.
In transparent server invocation, the port number is also supplied
to the server with -p option::

  $ groovyclient -Cp 1963 -e '...'

Log file
--------

The output from groovyserver is written to the following file::

  ~/.groovy/groovyserv/groovyserver-<port>.log

Tips
----

Following aliases might be useful. For bash::

  alias groovy=groovyclient

For Windows::

  doskey groovy=groovyclient $*

