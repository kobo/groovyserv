########
Welcome!
########

Overview
========

**GroovyServ makes Groovy's startup time much faster, by pre-invoking Groovy as a server.**

In software development with script languages, it's very important that repeat velocity of "try-and-run" is fast enough.
It is too long to wait for start-up of Groovy even for 1 second.
GroovyServ reduces startup time of the JVM and Groovy significantly.
It's of course dependent on environments, but in most case, it is *10 to 20 times faster than regular Groovy*.

See also :ref:`README <ref-readme>` (:ref:`Japanese <ref-readme_ja>`) and :ref:`Screencast <ref-screencast>`.

Feature
=======

GroovyServ supports following features:

- Faster invocation of Groovy script (about 10 times much).
- Transparent server invocation. If a server is not running when a client is invoked, the client invokes a server in background.
- Connect System.in/out/err through a socket stream.
- Simultaneous execution of scripts. See :ref:`FAQ <ref-faq-simultaneous>`.
- Trap a call of System.exit() and send the exit status to the client.
- Signal handling on client side (e.g. Ctrl-C for termination).
- Change current working directory as appropriately as possible. See :ref:`FAQ <ref-faq-cwd>`.
- Propagation of a CLASSPATH environment variable.
- C(native binary) and Ruby client, which can start up quickly.
- Available without Cygwin in Windows.
- Show a window to stop server process only in Windows.
- Propagation of environment variables from a client to a server.
- Automatic resolution of a "groovy" command path and GROOVYSERV_HOME (except on Windows).

