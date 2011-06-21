How to Install
==============

For each environment
--------------------

For Anywhere
^^^^^^^^^^^^

- :ref:`binary package (bin.zip) <ref-howtoinstall-binary>`

Only for Mac OS X
^^^^^^^^^^^^^^^^^

- :ref:`Macports <ref-howtoinstall-macports>`
- :ref:`Homebrew <ref-howtoinstall-homebrew>`

Only for Linux
^^^^^^^^^^^^^^

- :ref:`An unofficial RPM package <ref-howtoinstall-rpm>`

Only for Windows
^^^^^^^^^^^^^^^^

- Groovy's Windows-Installer (including GroovyServ): http://groovy.codehaus.org/Download


.. _ref-howtoinstall-binary:

Install from binary package
---------------------------

Download and expand GroovyServ distribution package from :ref:`Download page <ref-download>`, e.g. groovyserv-0.8-macosx-bin.zip to any directory::

    $ mkdir ~/opt
    $ cd ~/opt
    $ unzip groovyserv-0.8-macosx-bin.zip

And add bin directory to PATH environment variable.
For example in bash/bourne shell::

    export PATH=~/opt/groovyserv-0.8/bin:$PATH

That's all for preparing.
When you invoke groovyclient, groovyserver automatically starts in background.
At first, you might have to wait for a few seconds to startup::

    $ groovyclient -v
    Invoking server: '/opt/groovyserv-0.8/bin/groovyserver' -p 1961 
    Groovy home directory: (none)
    Groovy command path: /usr/local/bin/groovy (found at PATH)
    GroovyServ home directory: /opt/groovyserv-0.8
    GroovyServ work directory: /Users/ynak/.groovy/groovyserv
    Original classpath: (none)
    GroovyServ default classpath: /opt/groovyserv-0.8/lib/*
    Starting...
    groovyserver 75808(1961) is successfully started
    Groovy Version: 1.8.0 JVM: 1.6.0_24

If the binary downloaded doesn't work, try to build it from source code, according to :ref:`Build from source code <ref-howtobuild>`.

.. _ref-howtoinstall-macports:

Install via Macports
--------------------

To install::

    $ sudo port install groovyserv

See: http://www.macports.org/ (thanx, breskeby!)


.. _ref-howtoinstall-homebrew:

Install via Homebrew
--------------------

To install::

    $ brew install groovyserv

See: http://mxcl.github.com/homebrew/


.. _ref-howtoinstall-rpm:

Install from RPM package
------------------------

Currently we don't produce a RPM package officially. But there is the contributed SPEC file which is need to buld a RPM file on xxxxxxx.  So you can try to build it by yourself ;-)

See: `Build RPM file <ref-howtobuild-rpm>`

