package org.jggug.kobo.groovyserv;


import groovy.util.GroovyTestCase

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.Dump} class.
 * Before running this, you must start groovyserver.
 */
class EncodingTest extends GroovyTestCase {
  static final String NL = System.getProperty("line.separator");
  static final String FS = System.getProperty("file.separator");

  void testExec() {
    def cmd = """bin${FS}groovyclient.exe -e "println('����������')" """
    Process p = cmd.execute()
    p.waitFor();
    if (p.exitValue() == 201) {
      assert false : "server may not be running"
    }

    assert p.getInputStream().text == "����������"+NL
    p.getErrorStream().eachLine {
      assert false: "error output returned from the server: "+it
    }
  }

}
