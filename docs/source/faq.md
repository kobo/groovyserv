FAQ
====


### Why is the name of this software *GroovyServ*?

It's inspired by [Gnuserv](http://www.emacswiki.org/emacs/GnuClient) for Emacs.
A concept and an architecture of GroovyServ are very similar with it.


### What environments are supported?

Linux, Windows and Mac OS X are supported, so far.
GroovyServ can use on the environment which supports Java SDK, JNA and Go compiler.


### Why does GroovyServ use [JNA (Java Native Access)](https://jna.dev.java.net/)?

Because it's to change a current working directory (CWD) dinamically for each a request.
To be more specific, it's to call `chdir`(2) system call or `_chdir` win32 C runtime function.
You might think `user.dir` system property can be used for same purpose, as a pure Java way.
But it's not enough because JRE isn't used the `user.dir` for all of Java APIs.
JRE still uses a native CWD of a JVM process.
For example, `new FileInputStream(".")` is used not `user.dir` system property but a CWD.


### Why are user commands written in Go, not in Java or Groovy?

The purpose of GroovyServ is to reduce a startup time of a JVM process.
So, you can never use Java to implement especially a `groovyclient`.
Go programming language can cross-compile binaries for many environments on everywhere.
It's a very powerful and useful feature for a multi-platform tool like GroovyServ.


### Does GroovyServ for Windows require Cygwin?

No, it doesn't. Since 1.0, we rewrote user commands by Go language.
So now, GroovyServ are pefectly free from Cygwin.


### Can I use GroovyServ with Grape's @Grab annotation?

Yes, you can.
But you might see `OutOfMemoryError` of PermGen if you use transitive dependencies and run the script repeatedly.
It probably comes from a way to resolve a transitive dependency.
It seems different to resolve a direct dependency in Groovy.
You can use a `SystemClassLoader` parameter as a workaround:

```groovy
@GrabConfig(systemClassLoader=true)
@Grab("...")
```


### What is different GroovyServ from [NailGun](http://www.martiansoftware.com/nailgun/)?

The purpose of NailGun is same as GroovyServ's.
Besides, the architectures are very similar, too.
NailGun is a great product which is older than GroovyServ.
However, GroovyServ has some advantage, so far.

* Both
    * Reduces startup time of the JVM
    * Supports standard I/O streams via TCP/IP
    * Detoxicates System#exit()
* Only GroovyServ
    * Supports access control
    * Supports propagation of CLASSPATH
    * Supports propagation of environment variables
    * Returns exit code
    * Supports dynamic CWD
        * In NailGun, CWD is fixed at initial directory when a server process is started.
    * Supports interruption by CTRL-C
* Only NailGun
    * Implemented by pure Java
        * So you can use for bulit-in, like JRuby.

IMO, NailGun is useful to implement a built-in system for something.
On the other hand, GroovyServ is handy in case of running a script from console or other tools like a text editor.


### What is different GroovyServ from [Drip](https://github.com/ninjudd/drip)?

The purpose of Drip is same as GroovyServ's.
However, Drip has a very characteristic strategy:
Drip starts up new process for each a combination of same arguments of JVM, classpath, a main class and CWD. And it reuses the process in a certain period. The process is automatically shut down after the period.

This approach brings quite simple architecture and implementation.
It's so great and interesting.
But, Drip requires a `bash` command so far.
So, inevitably you can't use it without a `bash` command.
IMO, Drip is handy to run a same script with same arguments repeatedly multiple times in UN*X-like OS.


### Is GroovyServ dedicated for Groovy?

No.
you can use GroovyServ for other JVM languages.
For example, you can run a Clojure script like this:

```sh
$ CLASSPATH=/tmp/clojure.jar groovyserver
$ groovyclient -e 'import clojure.main;main.main(args)' -- -e "(println 'Clojure)"
Clojure
```


### Ctrl-C doesn't work in some cases. Why?

Is your script interruptable?
If not, GroovyServ can't stop it unfortunately.
See [User Guide](userguide.md#interrupt-by-ctrl-c).
