# User Guide

## Introduction

GroovyServ makes startup time quicker, by pre-invoking groovy as a TCP/IP server.
If you know gnuserv(gnudoit)/emacsserver/emacsclient, this is like that.

In the case of scripting in dynamic-type languages, quick response about invocation is very important.
Try-and-run cycles is repeated frequently than static-type languages, so sometimes 2 seconds or even a second might be intolerable.

GroovyServ reduces the startup time of JVM and Groovy runtime significantly.
In most situations it is 10 to 20 times faster than regular Groovy.
The following times are averages of 5 times which measured invocation in Groovy 2.1.5 on Mac OS X 10.8.4 (2.3GHz Intel Core i7).

|Command           |Result(sec)
|------------------|-----------
|Groovy            |0.803
|GroovyServ        |0.023


## Requirements

GroovyServ can be used in following environment/OS:

* Mac OS X 10.8 (Intel Mac)
* Ubuntu Linux 10.04
* Windows XP/7 with/without Cygwin
* And more

Version of JDK using at build is following:

* JDK 7u25

Version of Groovy is following:

* Groovy 1.8.6+


## How to use

You can use `groovyclient` command instead of `groovy` command.
If the server is not running yet, it automatically starts:

```
$ groovyclient -e "println('Hello, GroovyServ.')"
Hello, GroovyServ.
```

You can also invoke the server explicitly:

```
$ groovyserver
```

With -v option, the verbose messages are written into a log file.
It's useful to analyze a problem about invoking server:

```
$ groovyserver -v
```


## Differences from normal Groovy

* You can't concurrently use different current directory on a server.
    It also meets conditions if you invoke `groovyclient` simultaneously from two or more consoles.
    But if the running periods of each `groovyclient` are not overlapping, it can run without exception.

    If needed, you can simultaneously run multiple servers with different ports.

* A static variable is shared among Groovy program invocations.
    For instance, the system properties are shared:

    ```
    $ groovyclient -e "System.setProperty('a','abc')"
    $ groovyclient -e "println System.getProperty('a')"
    abc
    ```

    However, System.out, System.in and System.err are rightly used which are individually prepared for each invocation.

* Normally, environment variables of when `groovyserver` was invoked are used instead of those of `groovyclient` side.
    But if you specify `-Cenv/-Cenv-all` option, you can reflect the values of environment variables of client to the server.

    Only the `CLASSPATH` environment variable, however, whichever with or without those options, is always reflected to the server.
    The values are cleared at the end of each client invocation.
    It doesn't affect to next invocation.


## Security

GroovyServ has a possibility to run any Groovy script from client at default.
So server-client connection is limited to the connection from the same machine (localhost).
And the connection is authenticated by simple authtoken mechanism.
The authtoken file is stored at `~/.groovy/groovyserv/authtoken-<port>` and the file mode set to `0600`.
But in Windows environment, it is not effective.
So appropriately protect access to the file in Windows if needed.


## Environment variables

GroovyServ uses the following environment variables in runtime.

`HOME` (only on Linux or Mac OS X)
:   It's used to decide ~/.groovy/groovyserv directory path which is used for logging, storing a authtoken and a PID.
    It's set by default on unix-like OS.

`USERPROFILE` (only on Windows)
:   It's used to decide ~\.groovy\groovyserv directory path which is used for logging, storing a authtoken and a PID.
    It's set by default on Windows. If server process is invoked by groovyserver.bat, PID file doesn't exist.

`JAVA_HOME`
:   It's required for Groovy. Generally, it has been already set by installing Groovy.

`GROOVY_HOME` (optional)
:   It's used to specify groovy command path.
    If you've set groovy command into PATH environment variable, GroovyServ can find groovy command via PATH, so you don't have to set GROOVY_HOME.

`PATH` (optional)
:   It's used to specify groovy command path.
    If you've set GROOVY_HOME environment variable, GroovyServ uses it in order to find a groovy command, so you don't have to set the groovy command path to PATH.

`GROOVYSERV_HOST` (optional)
:   It's used to specify the host address for client.
    Alternately, you can specify it as a command option.

`GROOVYSERV_PORT` (optional)
:   It's used to specify the port number for server or client.
    Alternately, you can specify it as a command option.

`CLASSPATH` (optional)
:   CLASSPATH environment variable on where groovyserver starts up composes the environment variable of the groovyserver process, with Jars of GroovyServ.
    It's used as "default classpath" and affects invocations of every script.

    CLASSPATH environment variable on where groovyclient is invoked is transferred to the groovyserver and is dynamically set to the compiler's configuration of the script.
    (CLASSPATH environment variable of groovyserver is never modified.)
    The temporary classpath doesn't affect the next script invocation because it's reset on the tear-down phase of each script invocation.
    When searching a class, groovyserver's CLASSPATH environment variable is used priorly.
    The above behavior is quite same as groovyclient's -cp option.


## groovyclient's option

groovyclient's options start with "-C".
Those options are analyzed and consumed by groovyclient, and aren't passed to groovy command::

```
  -Ch,-Chelp                       show this usage
  -Cs,-Chost <address>             specify the host to connect to groovyserver
  -Cp,-Cport <port>                specify the port to connect to groovyserver
  -Ca,-Cauthtoken <authtoken>      specify the authtoken
  -Ck,-Ckill-server                kill the running groovyserver
  -Cr,-Crestart-server             restart the running groovyserver
  -Cq,-Cquiet                      suppress statring messages
  -Cenv <substr>                   pass environment variables of which a name
                                   includes specified substr
  -Cenv-all                        pass all environment variables
  -Cenv-exclude <substr>           don't pass environment variables of which a
                                   name includes specified substr
  -Cv,-Cversion                    display the GroovyServ version
```

Since v0.12, groovyclient.sh written by bash script has been added.
Now you can easily use GroovyServ on the environment where there is no appropriate native client and ruby isn't installed.
But there are some restrictions due to the limitation of the power of bash.

 - Signal handling on client side (Ctrl+C)
 - System.in from client
 - Distinguishable stdout from stderr on client (all responses to stdout)
 - Status code from server ($?)

They may be improved at future version.
If you want to use a full featured client, use `groovyclient.rb`.
Or, run `groovyserv-setup-nativeclient` command including platform independent package in order to build a native client and replace `groovyclient` with it.
See [Build from source code in User Guide](howtobuild.md).


## groovyserver's option

groovyserver's options are as follows:

```
  -v                       verbose output to the log file
  -q                       suppress starting messages
  -k                       kill the running groovyserver (unsupported in groovyserver.bat)
  -r                       restart the running groovyserver (unsupported in groovyserver.bat)
  -p <port>                specify the port to listen
  --allow-from <addresses> specify optional acceptable client addresses (delimiter: comma)
  --authtoken <authtoken>  specify authtoken (which is automatically generated if not specified)
```

## Start and stop groovyserver

There are two ways to invoke `groovyserver`; the one is, called "explicit server invocation", the way of using `groovyserver` or `groovyserver.bat`.
The another is, called "transparent server invocation", the way of just using `groovyclient`.
If `groovyserver` hasn't run yet, `groovyclient` automatically invokes `groovyserver` at the background.

The commands for explicit server invocation are:

* `groovyserver`      (for Mac OS X, Linux, Windows with Cygwin)
* `groovyserver.bat`  (for Windows without Cygwin)

Following table shows the availability of those commands: (OK: Available, N/A: Not available)

Script            |Windows w/ Cygwin |Windows w/o Cygwin |Mac OS X, Linux
------------------|------------------|-------------------|----------------
groovyserver      |OK                |N/A                |OK
groovyserver.bat  |OK                |OK                 |N/A

`groovyserver.bat` doesn't support `-r` and `-k` options for technical reasons.
So, on the command line, You can neither stop nor restart the server started by `groovyserver.bat`.
Instead, a minimized window is shown when server is started by `groovyserver.bat`.
You can stop the server by closing the window.
As a result, then you can restart server by invoking `groovyclient` as transparent server invocation.

On Cygwin, `groovyclient` internally uses `groovyserver.bat` for transparent server invocation.
Therefore, the behavior on Cygwin is as follows:

- In the case of server explicitly invoked by `groovyserver` shell script, you can stop or restart the server by invoking `groovyserver` shell script with `-k` or `-r` options.
- In the case of server explicitly invoked by `groovyserver.bat` (bat file), you can stop the server by closing the window of the server.
- In the case of groovyserver transparently invoked by `groovyclient.exe`, you can stop the server by closing the window of the server.

It seems be confusing enough.
So, we are considering to support `-r` and `-k` options of groovyserver.bat.

In transparent server invocation, you cannot supply options(e.g. `-v`) for `groovyserver` or `groovyserver.bat` which is invoked internally by `groovyclient`.
If you need, explicitly invoke `groovyserver` with options.


## Propagation of environment variable

With `-Cenv` option of `groovyclient`, you can pass environment variables of which a name includes the specified substring to `groovyserver`.
The values of those variables on the client process are sent to the server process, and the values of same environment variables on the server are set to or overwritten by the passed values.
This feature is especially useful for tools (e.g. IDE, TextMate, Sublime Text 2) which invoke an external command written by Groovy, and which uses environment variables to pass parameters to the command.

When you specify the option `-Cenv-all`, all environment variables of the `groovyclient` process are sent to the groovyserver.
Additionally with the option `-Cenv-exclude`, the variables of which a name includes specified substring are excluded.

If you specify option:

```
-Cenv SUBSTRING
```

the set of environment variables sent to the server are determined by the following pseudo code:

```
allEnvironmentVariables.entrySet().findAll {
    it.name.contains("SUBSTRING")
}
```

Consider the combination of `-Cenv`, `-Cenv-all` and `-Cenv-exclude`, like:

```
-Cenv SUBSTRING
-Cenv-all
-Cenv-exclude EXCLUDE_SUBSTRING
```

The result of the following pseudo code are sent to the server:

```
allEnvironmentVariables.entrySet().findAll {
    if (isSpecifiedEnvAll || it.name.contains("SUBSTRING")) {
        if (!it.name.contains("EXCLUDE_SUBSTRING")) {
            return true
        }
    }
    return false
}
```

Note that the environment variables which is set to the server remain after the `groovyclient` terminates.
And modifying an environment variable on a server are not thread-safe.
So when multiple `groovyclient` instances are invoked simultaneously, a variable which one of them needs might be overwritten by another client subsequently invoked.


## Port number

As a default, TCP port number which is used for communication between a server and a client is `1961`.
To change a port number used by a server, you can use `GROOVYSERV_PORT` environment variable or `-p` option.
The `-p` option is used more prior than `GROOVYSERV_PORT` environment variable:

```
$ export GROOVYSERV_PORT=1963
$ groovyserver
```

or:

```
$ groovyserver -p 1963
```

On the other hand, for a groovyclient, you can use `-Cp` option instead of `-p` which is used by Groovy and `GROOVYSERV_PORT` environment variable.
In transparent server invocation, the port number is also supplied to the server with `-p` option::

```
$ groovyclient -Cp 1963 -e '...'
```


## Log file

The output from server is written to the following file:

* Linux, Mac OS X: `$HOME/.groovy/groovyserver/groovyserver-<port>.log`
* Windows: `%USERPROFILE%/.groovy/groovyserver/groovyserver-<port>.log`


## Remote access

When invoking `groovyserver`, you specify a client address as a parameter.
For example, assume that the server's ip address is `192.168.1.1` and the client's one is `192.168.1.2`:

```
server$ groovyserver --allow-from 192.168.1.2
```

When invoking `groovyclient` on the client, the authtoken which is stored in `~/.groovy/groovyserv/authtoken-<port>` must be specified:

```
server$ cat ~/.groovy/groovyserv/authtoken-1961
7d3dc4d7a2b8b5ca
```

```
client$ groovyclient -Chost 192.168.1.1 -Cauthtoken 7d3dc4d7a2b8b5ca -e "println('Hello from remote client.')"
Hello from remote client.
```

You can also provide an authtoken explicitly as you want.
But it might cause less security if the authtoken is too simple:

```
server$ groovyserver --allow-from 192.168.1.2 --authtoken GROOVYSERV
server$ cat ~/.groovy/groovyserv/authtoken-1961
GROOVYSERV
client$ groovyclient -Chost 192.168.1.1 -Cauthtoken GROOVYSERV -e "println('Hello from remote client.')"
Hello from remote client.
```

When invoking groovyclient with `-Chost` option, you cannot use options to control server process in localhost like `-Cr` option.
You can provide multi ip addresses of clients to `--allow-from` option with a comma as a delimiter.


## Tips

Following aliases might be useful. For bash::

```
alias groovy=groovyclient
```

For Windows:

```
doskey groovy=groovyclient $*
```
