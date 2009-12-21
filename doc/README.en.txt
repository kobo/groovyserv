GroovyServer README

==========
Introduction
==========

GroovyServer reduce startup time of groovy runtime by running groovy
as a TCP/IP server. If you know gnuserv/gnuclient of GNU Emacs,
groovyserver/groovyclient is almost the same as it.

====================
Why groovyserver?
====================

If you use groovy as scripting language, turn around time of
try-and-run loop is very important, because such routine work is done
very frequently. Quick initiation are more necessary in dynamic
language, more than in static type languages, because you have to find
type-error at runtime, not compile time,  in many cases.

Some IDE might might support incremental compilation, but for writing
and small script like tools, filters, by using editor, groovyserver is
useful.

==========
System requirements
==========

In Windows environment, cygwin is required.
I also tested it in MacOS X 10.x enviornment.

================
Version of Groovy
================

It confirms the operation by 1.7- RC-1 and 1.6.6. 

========
Install
========

Expand GroovyServer distribution package.  "$GROOVYSERV_HOME" means
the directory where expand the GroovyServer distribution package.

  $ unzip groovyserv-1.x.x.zip

========
Compile
========

To compile groovyserver, Maven is required. To compile from source
package:

$ cd $GROOVYSERV_HOME
$ mvn compile

========
Test
========

Set PATH environment variable to:

 PATH=$GROOVYSERV_HOME/bin:$PATH

For test installation is succeeded, run following:

$GROOVYSERV_HOME/bin/groovyclient.exe -v

if no problem, followings will output :

 Groovy Version: 1.7-rc-2 JVM: 1.6.0_13

First time, You have to wait for a few seconds because GroovyServer
starts background.

================
How to use
================

You can use 'groovyclient' command as replacement of 'groovy'
command. And if you like, following aliases might useful.

 alias groovy=groovyclient

Following aliases might be useful also.

 alias groovyc="groovyclient -e 'org.codehaus.groovy.tools.FileSystemCompiler.main(args)'"
 alias groovysh="groovyclient -e 'groovy.ui.InteractiveShell.main(args)'"
 alias groovyConsole="groovyclient -e 'groovy.ui.Console.main(args)'"
 alias grape="groovyclient -e 'org.codehaus.groovy.tools.GrapeMain.main(args)'"

================
Differences
================

* You can't use multiple different current directory concurrentry.  If
  execute two or more groovy client at the same time, and assign
  different current directry like follwing, using subshell, the
  current directory specified for groovyclient that started after
  overwrite before one.

  $ groovyclient  ... | (cd /tmp; groovyclient .. ) 

* A static variable is shared by each groovy program executions.  For
  instance, the system property is shared.

 $ groovy -e "System.setProperty('a','abc')"
 $ groovy -e "println System.getProperty('a')"
 abc

* -l option (listen TCP port) cannot be used.

* Environment variables when groovyclient is invoked is not used.
  Especially, CLASSPATH is not used. You have to specify it to -cp
  option of groovyclient.

* Standard output and standard input cannot be treated from a new
  thread.[now working]

* Exit status '201' is reserved. If yo want to exit the Groovy script
  by status 201, for example "System.exit(201)", the status is
  converted to fixed number '1' (same as System.exit(1)).

===================
groovyserver Options
===================

-v Tedious display. Debugging information etc. are displayed. 
-k Starting GroovyServer is ended. 

===================
groovyclient Options
===================

[TBD]

================
Log file
================

Groovyserver make logs to:

~/.groovy/groovyserver/<process ID>.log

================
Security
================

Remote execution from groovyclient which running on other machine, but
invocation from other user from same machine on which GroovyServer
running.
