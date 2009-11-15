package org.jggug.kobo.groovyserv

import groovy.util.GroovyTestCase

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.Dump} class.
 * Before running this, you must start groovyserver.
 */
class ExecTest extends GroovyTestCase {
    static final String NL = System.getProperty("line.separator");
    static final String FS = System.getProperty("file.separator");

    void testExec() {
        Process p = ("bin"+FS+"groovyclient.exe -e \"println('hello')\"").execute()
        assert p.getInputStream().text == 'hello\n'
        p.getErrorStream().eachLine {
            if (it == 'connect: Connection refused') {
                println "<"+it+">"
                assert false : "server may not running"
            }
            assert false: "error output returned from server: "+it
        }

    }

}
