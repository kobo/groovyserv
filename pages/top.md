# Quick startup, by pre-invoking Groovy as a server

In software development with script languages, it's very important that repeat velocity of "try-and-run" is fast enough.
It is too long to wait for start-up of Groovy even for 1 second.
GroovyServ reduces startup time of the JVM and Groovy significantly.
It's of course dependent on environments, but in most case, it is *10 to 20 times faster than regular Groovy*.

See also [User Guide](./userguide.html).

![demo](./images/groovyserv-demo.gif)


# Features

GroovyServ supports following features:

- Faster invocation of Groovy script (about 10 times much).
- Transparent server invocation. If a server is not running when a client is invoked, the client invokes a server in background.
- Connect `System.in/out/err` through a socket stream.
- Simultaneous execution of scripts. See :ref:`FAQ <ref-faq-simultaneous>`.
- Trap a call of System.exit() and send the exit status to the client.
- Signal handling on client side (e.g. Ctrl-C for termination).
- Changes current working directory as appropriately as possible. See :ref:`FAQ <ref-faq-cwd>`.
- Propagation of a `CLASSPATH` environment variable.
- Propagation of environment variables from a client to a server.
- Shell script (limitted version), C (native binary) and Ruby client.
- Available without Cygwin in Windows.
- Shows a window to stop server process only in Windows.
- Automatic resolution of a "groovy" command path.
