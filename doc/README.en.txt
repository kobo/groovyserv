GroovyServer 0.1 README
March 3th, 2010

==========
Introduction
==========

GroovyServer make startup time quicker, by pre-invoking groovy as a
TCP/IP server.

(If you know gnuserv/gnuclient, this is like that.)

For writing script, for example by using groovy, quick response is
very important. Because it has less static type checking, try-and-run
loop is repeated frequently. Sometimes 2 seconds or even 1 second
might be intolerable.

==========
System requirements
==========

GroovyServer develoed for follwing environment.

 - Windows XP+cygwin
 - Mac OS X 10.5/6
 - Linux

Please report if it runs on other environment/OS.

=========
Language
=========

The Server is written in Java with JNA(Java Native Access.)
Two type of clients are implemented, one is written in C
and another is written in Ruby.

================
Security
================

GroovyServer have a possibility to run any groovy script from client.
So server-client connection is limited to the connection from the same
machine (localhost). And the connection is authenticated by simple
cookie mechanism. The cookie file is stored at ~/.groovy/groovyserv/key
and the file mode set to 0400. But in Windows environment, it have no
effect. So set suitable protection to the file if needed in Windows.

================
Version of Groovy
================

 1.6 or later.

================================
Install from binary package
================================

Download and expand GroovyServer distribution package

  groovyserv-0.1-SNAPSHOT-win32-bin.zip

or 

  groovyserv-0.1-SNAPSHOT-macosx-bin.zip

to any directory. For example:

 > mkdir ~/opt
 > cd ~/opt
 > unzip -l groovyserv-0.1-SNAPSHOT-win32-bin.zip

and add bin directory to PATH environment variables.
bash/bourne shell example is:

 export PATH=~/opt/groovyserv-0.1-SNAPSHOT/bin:$PATH

Setting is all. And then invoke groovyclient then groovyserver starts
in background. First time, you might have to wait for a few seconds to
startup.

 > groovyclient -v
 starting server..
 Groovy Version: 1.7.0 JVM: 1.6.0_13


========================
Build from source
========================

Download and expand GroovyServer source package
groovyserv-0.1-SNAPSHOT-src.zip to any directory.
For example:

 > mkdir -p ~/opt/src
 > cd ~/opt/src
 > unzip -l groovyserv-0.1-SNAPSHOT-src.zip

compile with Maven2.

 > cd ~/opt/src/groovyserv-0.1-SNAPSHOT/
 > mvn clean compile

In Mac OS or Linux environment,

  ~/opt/src/groovyserv-0.1-SNAPSHOT/bin/groovyclient

will generated. In Windows environment

  ~/opt/src/groovyserv-0.1-SNAPSHOT/bin/groovyclient.exe

will generated. If some tests fail,

 > mvn -Dmaven.test.skip=true clean compile

make skip tests.

========
TIPS
========

Following aliases might be useful.

  alias groovy=groovyclient
  alias groovyc="groovyclient -e 'org.codehaus.groovy.tools.FileSystemCompiler.main(args)'"
  alias groovysh="groovyclient -e 'groovy.ui.InteractiveShell.main(args)'"
  alias groovyConsole="groovyclient -e 'groovy.ui.Console.main(args)'"

================
How to use
================

You can use 'groovyclient' command as a replacement of 'groovy'
command. if the server is not running, it starts automatically.

You can invoke it explicitly.

 > groovyserver

About groovyserver command options will be described in following.

================================
Restriction/Differences
================================

* You can't use different current directory concurrently on a server.
  If execute following, it makes exception.

  > groovyclient  ... | (cd /tmp; groovyclient .. ) 

  In this case, following exception thrown:

    org.jggug.kobo.groovyserv.GroovyServerException: Can't change
    current directory because of another session running on different
    dir: ....

  If you open 2 or mote console, and use groovyclient simultaneously,
  the situation is same. But if the running span is not overlapped,
  there is no problem.

  If needed, you can run multiple groovy server for separated port.

* A static variable is shared among each groovy program
  executions. For instance, the system property is shared.

 > groovy -e "System.setProperty('a','abc')"
 > groovy -e "println System.getProperty('a')"
 abc

* Environment variables when groovyclient is invoked is not used.
  Groovyserver's environment is used.
  But CLASSPATH is special, it passed to server and client side
  value of CLASSPATH is effective.

* Groovy's -cp option value and CLASSPATH value is passed to the
  server and effective. But it only add to the environment and never
  removed when the groovy session is over.

===================
groovyserver Options
===================

Options are following:

  -v verbose output. Debugging information etc. are displayed. 
  -q quiet.(default)
  -k kill the running GroovyServer.
  -r restart GroovyServer.
  -p <port> specify the port for GroovyServer.

================
Port number
================

To change the port number for which GroovyServer used, you can set it
to GROOVYSERVER_PORT environment variable.

 > export GROOVYSERVER_PORT=1963

Port number can be specified by using -p option of groovyclient.  -p
option value is treated prior then GROOVYSERVER_PORT environment
variable.

================
Log file
================

When the GroovyServer invoked by client, those output are logged to:

~/.groovy/groovyserver/<Process ID>-<Port Number>.log

================
Support
================

- github URL

================
Presented by
================

- Kobo Project.
- NTT Software Corp.
