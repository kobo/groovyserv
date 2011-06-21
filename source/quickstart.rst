Quick Start
===========

After Installing GroovyServ
---------------------------

Now if you want to use GroovyServ, you can use the groovyclient command instead of an original groovy command::

    $ groovy -e "println('Hello, Groovy!')"
    $ groovyclient -e "println('Hello, GroovyServ!')"

How faster is GroovyServ than Groovy?


Two commands for you
--------------------

GroovyServ has two commands, groovyclient and groovyserver.


groovyclient
^^^^^^^^^^^^

The groovyclient is a front controller for user. When you invoke it, it passed the arguments and standard input stream to the back-end's groovyserver (which is automatically invoked if not exists).

In many cases, groovy command can be just replaced with groovyclient::

    $ groovy -e "println('Hello, Groovy!')"
    $ groovyclient -e "println('Hello, GroovyServ!')"

Or::

    $ groovy hello.groovy
    $ groovyclient hello.groovy

For more topics, read the :ref:`User Guide <ref-userguide>`.


groovyserver
^^^^^^^^^^^^

The groovyserver is the engine to run your Groovy script. By invoking groovyclient, groovyserver is automatically invoked and stationed permanently. But you can also explicitly invoke it with detail options.

For example, when you want to kill the groovyserver process (e.g. because there isn't enough memory)::

    $ groovyserver -k

Or, when you want to restart the process and turn debug mode on (e.g. because it seems something wrong)::

    $ groovyserver -r -v

Of course, in usual case, you don't have to use the groovyserver command.
For more topics, read the :ref:`User Guide <ref-userguide>`.

