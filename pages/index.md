<img src="./images/groovyserv-logo.png" class="brand-logo"/>

# <i class="fa fa-forward"></i>Quick startup, by using a JVM process running in background

GroovyServ reduces startup time of the JVM for runnning Groovy significantly.
It depends on your environments, but in most case, it's **10 to 20 times faster than regular Groovy**.

![demo](./images/groovyserv-demo.gif)


## <i class="fa fa-star"></i>Features

GroovyServ provides following features:

* Quick startup of Groovy script (about 10 to 20 times much).
* Transparent server operation.
    - If a server is not running when a client is invoked, the client runs a server in background.
* `System.in/out/err` is available through a socket stream.
* Trap a call of `System.exit()` and send the exit status to the client
    - Server process keeps working.
* Signal handling on a client side.
    - Ctrl-C terminates only a client process.
* Changes a current working directory as appropriately as possible.
* Automatic propagation of a `CLASSPATH` environment variable from a client to a server.
* Selectable propagation of any environment variables from a client to a server.
* User commands written by [Go programming language](http://golang.org/).
* Works on Linux, Mac and even Windows without Cygwin.
