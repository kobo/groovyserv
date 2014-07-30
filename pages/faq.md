# FAQ


### Why is the name of this software 'GroovyServ'?

It's inspired by [Gnuserv](http://www.emacswiki.org/emacs/GnuClient) for Emacs.
Don't you know it?


### What environments are supported?

Linux, Windows and Mac OS X are supported, so far.
GroovyServ can use on the environment which supports both JDK and JNA.


<span id="cwd"></span>
### Why does GroovyServ use [JNA(Java Native Access)](https://jna.dev.java.net/)?

Because to change current working directory.
To be more specific, to call `chdir`(2) system call or `_chdir` win32 C runtime function.
You might think `user.dir` system property can be used for same purpose,
in Pure Java way, but it is not enough because it has no effect to all of
Java functionality about current working directory.
For example, it has no effect to call `new FileInputStream(".")`.


<span id="simultaneous"></span>
### What happens if I invoke two Groovy scripts which have different current working directory at the same time?

For example, when two Groovy scripts concurrently are invoked by using sub-shell and pipe, an exception occurs.

```
$ (cd /tmp/dir1; groovyclient read.groovy ) | ( cd  /tmp/dir2; groovyclient output.groovy )
```

Instead, the following works well:

```
$ (cd /tmp/dir1; groovyclient read.groovy ) | ( cd /tmp/dir1; groovyclient output.groovy )
```

Or, you can also run with another groovyserver process.

```
$ (cd /tmp/dir1; groovyclient -Cport 1963 read.groovy ) | ( cd /tmp/dir2; groovyclient -Cport 1964 output.groovy )
```


### Does GroovyServ for Windows require Cygwin?

No, it doesn't. In GroovyServ 0.1 groovyclient.exe required cygwin.dll.
In GroovyServ 0.2, however, we rewrote it to be able to compile with MinGW.
So now, it doesn't require cygwin.dll and Cygwin environment at all.
New groovyserver.bat are provided instead of groovyserver shell script.


### Why is groovyclient written in C? Why isn't it written in Java?

The purpose of GroovyServ is to reduce startup-time of Groovy script and quick response is the most important factor.
So we could not use Java for groovyclient.


### What is the server-client communication protocol?

It is a original proprietary protocol. To know the sequence of the protocol,
please read comments in ClientProtocols.groovy or groovyclient.rb for details.


### If the JVM process is shared by each script session, static variables are shared too?

Yes and No.
If the static variables are defined in the script or Groovy classes you run,
they are defined on a GroovyClassLoader created for each script session.
In this case, static variables aren't shared for each script because class loaders are different.
On the other hand, if the static variables are defined by system/bootstrap class loaders, they are shared.
For example, the values which you stored in System.getProperties() are shared among all scripts invoked on the GroovyServ.


### Can I use GroovyServ with Grape's @Grab annotation?

Yes, you can.
But you might see OutOfMemoryError of PermGen if you use transitive dependencies and run the script repeatedly.
It is probably comes from the way to resolve transitive dependencies is different of resolving direct dependencies in Groovy.
You can avoid this error by specify SystemClassLoader to use:

```
@GrabConfig(systemClassLoader=true)
@Grab("..")
```


## When I built GroovyServ by myself in Windows, some errors occured. What's required?

To build it in Windows, you need Cygwin and gcc-3 and MinGW.
See [Build from source code in User Guide](./howtobuild.html).
