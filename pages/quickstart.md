# Quick Start

## After Installing GroovyServ

Now, you can use a `groovyclient` command instead of an original `groovy` command:

```
$ groovy -e "println 'Hello, Groovy.'"
Hello, Groovy.

$ groovyclient -e "println 'Hello, GroovyServ.'"
Hello, GroovyServ.
```

Wow! How faster is GroovyServ than Groovy?


## Two commands available for you

GroovyServ provides two commands: a `groovyclient` and a `groovyserver`.


### groovyclient

A `groovyclient` is a main command for a user.
When you run it, it passed an specified arguments and a standard input stream to a backend's server process (which is automatically started up if not exists).

In many cases, a call of a `groovy` command can be simply replaced with a `groovyclient`:

```
$ groovy -e "println 'Hello, Groovy.'"
Hello, Groovy.

$ groovyclient -e "println 'Hello, GroovyServ.'"
Hello, GroovyServ.
```

Or

```
$ cat hello.groovy
println 'Hello, Groovy.'

$ groovy hello.groovy
Hello, Groovy.

$ groovyclient hello.groovy
Hello, GroovyServ.
```

For further information, read [User Guide](./userguide.html).


### groovyserver

A `groovyserver` controls a server process which runs your Groovy script.
By running `groovyclient`, the server is automatically started up and it keeps running permanently.
Though, you often want to run it explicitly with detail options.
For example, if you want to kill a server process because there isn't enough memory, you can do like this:

```
$ groovyserver -k
```

Or, when you want to restart a process and turn debug mode on because it seems something wrong:

```
$ groovyserver -r -v
```

Of course, in usual case, you don't have to use the `groovyserver` command.
For further information, read [User Guide](./userguide.html).
