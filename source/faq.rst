.. _ref-faq:

FAQ
===

.. contents::

.. _ref-faq-cwd:

Why does GroovyServ use JNA(Java Native Access)?
------------------------------------------------
Q:
    Why does GroovyServ use `JNA(Java Native Access) <https://jna.dev.java.net/>`_ ?
A:
    Because to change current working directory.
    To be more specific, to call chdir(2) system call or _chdir win32 C runtime function.
    You might think 'user.dir' system property can be used for same purpose,
    in Pure Java way, but it is not enough because it has no effect to all of
    Java functionality about current working directory.
    For example, it has no effect to call new FileInputStream(".").

.. _ref-faq-simultaneous:

What happens if I invoke two Groovy scripts which have different current working directory at the same time?
------------------------------------------------------------------------------------------------------------
Q:
    What happens if I invoke two Groovy scripts which have different current working directory at the same time?
    For example, using sub-shell and pipe, two Groovy scripts concurrently are invoked::

        $ (cd /tmp; groovyclient read.groovy ) | ( cd  /var/tmp; groovyclient output.groovy )
A:
    This is the restricted case. Exception occurs when the second groovyclient command is invoked. Instead, the following works well::

        $ (cd /tmp; groovyclient read.groovy ) | ( cd /tmp; groovyclient output.groovy )

What environments are supported?
--------------------------------
Q:
    What environments are supported?
A:
    So far Ubuntu Linux, Windows and Mac OS X are supported.
    Basically, GroovyServ can run on the environment which supports JDK and JNA.

Are there packages for MacPorts, \*.deb, \*.rpm?
------------------------------------------------
Q:
    Are there packages for MacPorts, \*.deb, \*.rpm?

A:
    Partially Yes! GroovyServ is now available on the MacPorts, Homebrew.
    The others may be under construction... We hope you'll do it!

Does GroovyServ for Windows require Cygwin?
-------------------------------------------
Q:
    Does GroovyServ for Windows require Cygwin?
A:
    No, it doesn't. In GroovyServ 0.1 groovyclient.exe required cygwin.dll.
    In GroovyServ 0.2, however, we rewrote it to be able to compile with MinGW.
    So now, it doesn't require cygwin.dll and Cygwin environment at all.
    New groovyserver.bat are provided instead of groovyserver shell script.

Why is groovyclient written in C? Why isn't it written in Java?
---------------------------------------------------------------
Q:
    Why is groovyclient written in C? Why isn't it written in Java?
A:
    The purpose of GroovyServ is to reduce startup-time of Groovy script and quick response is the most important factor.
    So we could not use Java for groovyclient.

What is the server-client communication protocol?
-------------------------------------------------
Q:
    What is the server-client communication protocol?
A:
    It is a original proprietary protocol. To know the sequence of the protocol,
    please read comments in ClientProtocols.groovy or groovyclient.rb for details.

If the JVM process is shared by each script session, static variables are shared too?
-------------------------------------------------------------------------------------
Q:
    If the JVM process is shared by each script session, static variables are shared too?
A:
    Yes and No.
    If the static variables are defined in the script or Groovy classes you run,
    they are defined on a GroovyClassLoader created for each script session.
    In this case, static variables aren't shared for each script because class loaders are different.
    On the other hand, if the static variables are defined by system/bootstrap class loaders, they are shared.
    For example, the values which you stored in System.getProperties() are shared among all scripts invoked on the GroovyServ.

How can I invoke the original groovy command when 'groovy' has been aliased to groovyclient?
--------------------------------------------------------------------------------------------
Q:
    How can I invoke the original groovy command when 'groovy' has been aliased to groovyclient?
A:
    Append the backslash as prefix like \groovy(for bash or csh). Or specify groovy command in full path.

Where is there a log file?
--------------------------
Q:
    Where is there a log file?
A:
    Linux, Mac OS X:
        $HOME/.groovy/groovyserver/groovyserver-<port>.log
    Windows:
        %USERPROFILE%/.groovy/groovyserver/groovyserver-<port>.log

How can I restart or kill groovyserver?
---------------------------------------
Q:
    How can I restart or kill groovyserver?
A:
    In Mac OS X, Linux and Cygwin environment, you can use groovyserver shell script with -r (restart) and -k (kill) options. **In addition, from GroovyServ 0.5, you can also use -Cr, and -Ck options with groovyclient command.**
    In Windows, if you invoke directly groovyserver.bat or invoke groovyclient with a transparent server invocation which uses
    groovyserver.bat, groovyserver create a new minimum window for control the server. By closing the window you can stop the groovyserver.

Why is the name of this software 'GroovyServ'?
----------------------------------------------
Q:
    Why is the name of this software 'GroovyServ'?
A:
    Because this is like `Gnuserv <http://www.emacswiki.org/emacs/GnuClient>`_ for Emacs. Do you know it?

Can I use GroovyServ with Grape's @Grab annotation?
---------------------------------------------------
Q:
    Can I use Grape's @Grab annotation in GroovyServ?
A:
    Yes, you can.
    But you might see OutOfMemoryError of PermGen if you use transitive dependencies and invoke that script repeatedly.
    It is probably comes from the way to resolve transitive dependencies is different of resolving direct dependencies in Groovy.
    You can avoid this error by specify SystemClassLoader to use::

      @GrabConfig(systemClassLoader=true)
      @Grab("..")


Can groovyclient connect to groovyserver running on remote server?
------------------------------------------------------------------
Q:
    Can groovyclient connect to groovyserver running on remote server?
A:
    No, it can't.
    A connection from the host other than localhost is inhibited for security reasons.
    Only a client which can read a secret cookie file created by the groovyserver is allowed to connect.
    This restriction is needed because groovyserver invokes any script containing even evil code through TCP socket.

Can I use JQS(Java Quick Start) with GroovyServ at the same time?
-----------------------------------------------------------------
Q:
    Can I use JQS(Java Quick Start) with GroovyServ at the same time?
A:
    Yes, you can. No problem.

I installed GroovyServ, but it doesn't work.
--------------------------------------------
Q:
    I installed GroovyServ, but it doesn't work.
A:
    I think it is caused by either reasons as follows:

    - GroovyServ requires HOME/USERPROFILE, JAVA_HOME and GROOVY_HOME environment variables. Please confirm if they exists.
      See :ref:`Environment variables in README <ref-readme-env>`.

    - An executable file we built isn't appropriate to your environment. Please build it by yourself.
      See :ref:`Build from source code in README <ref-readme-build>`.

When I built GroovyServ by myself in Windows, some errors occured. What's required?
-----------------------------------------------------------------------------------
Q:
    When I built GroovyServ by myself on Windows, some errors occured. What's required?
A:
    To build it in Windows, you need Cygwin and gcc-3 and MinGW.
    See :ref:`Build from source code in README <ref-readme-build>`.

