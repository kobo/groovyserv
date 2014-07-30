# Quick Start

## After Installing GroovyServ

Now, you can use `groovyclient` command instead of an original `groovy` command:

```
$ groovy -e "println('Hello, Groovy.')"
$ groovyclient -e "println('Hello, GroovyServ.')"
```

Wow! How faster is GroovyServ than Groovy?


## Two commands for you

GroovyServ has two commands: `groovyclient` and `groovyserver`.


### groovyclient

The `groovyclient` is a main command for user.
When you invoke it, it passed the arguments and standard input stream to the back-end's `groovyserver` (which is automatically invoked if not exists).

In many cases, `groovy` command can be simply replaced with `groovyclient`:

```
$ groovy -e "println('Hello, Groovy.')"
$ groovyclient -e "println('Hello, GroovyServ.')"
```

Or

```
$ groovy hello.groovy
$ groovyclient hello.groovy
```

For further information, read [User Guide](./userguide.html).


### groovyserver

The `groovyserver` is the engine to run your Groovy script.
By invoking `groovyclient`, `groovyserver` is automatically started up and kept running permanently.
You can also explicitly run it with detail options.
For example, when you want to kill the `groovyserver` process because there isn't enough memory:

```
$ groovyserver -k
```

Or, when you want to restart the process and turn debug mode on because it seems something wrong:

```
$ groovyserver -r -v
```

Of course, in usual case, you don't have to use the `groovyserver` command.
For further information, read [User Guide](./userguide.html).
