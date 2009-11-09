package org.jggug.kobo.groovyserv

import groovy.util.GroovyTestCase

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.Dump} class.
 */
class ExecTest extends GroovyTestCase {
    static final String NL = System.getProperty("line.separator");

    void testExec() {
        Process p = "bin\\groovyclient.exe -e 'println \"hello\" '".execute()
        println "----------------"
        p.getInputStream().eachLine {
            assert it == "hello"
        }
        println "----------------"
        p.getErrorStream().eachLine {
            if (it == 'connect: Connection refused') {
                println "<"+it+">"
                assert false : "server may not running"
            }
            assert false: "error output returned from server: "+it
        }

    }

}
