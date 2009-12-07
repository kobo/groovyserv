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

    assert p.getInputStream().text == "hello"+NL
    p.getErrorStream().eachLine {
      if (it == 'connect: Connection refused') {
        println "<"+it+">"
        assert false : "server may not be running"
      }
      assert false: "error output returned from the server: "+it
    }

  }

  void testMultiLineWrite() {
    def cmd = """bin${FS}groovyclient.exe -e "[0,1,2].each{println it}" """
    Process p = cmd.execute()
    assert p.getInputStream().text == "0"+NL+"1"+NL+"2"+NL
    p.getErrorStream().eachLine {
      if (it == 'connect: Connection refused') {
        assert false : "server may not running"
      }
      assert false: "error output returned from server: "+it
    }
  }

  void testMultiLineRead() {
    def cmd = """bin${FS}groovyclient.exe -e "System.in.eachLine{println it+it}" """
    Process p = cmd.execute()
    def os =  p.getOutputStream()
    os.write("A${NL}B${NL}".getBytes())
    os.close()
    assert p.getInputStream().text == "AA"+NL+"BB"+NL
    p.getErrorStream().eachLine {
      if (it == 'connect: Connection refused') {
        println "<"+it+">"
        assert false : "server may not running"
      }
      assert false: "error output returned from server: "+it
    }
  }

}
