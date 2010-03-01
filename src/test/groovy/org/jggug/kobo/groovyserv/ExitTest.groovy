package org.jggug.kobo.groovyserv


import groovy.util.GroovyTestCase

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.Dump} class.
 * Before running this, you must start groovyserver.
 */
class ExitTest extends GroovyTestCase {
  static final String NL = System.getProperty("line.separator");
  static final String FS = System.getProperty("file.separator");

  void testExit1() {
    def cmd = """bin${FS}groovyclient.exe -e "System.exit(1)" """
    Process p = cmd.execute();
    p.waitFor();
    if (p.exitValue() == 15) {
      assert false : "server may not be running"
    }
    assert p.exitValue() == 1
  }

  void testExit33() {
    def cmd = """bin${FS}groovyclient.exe -e "System.exit(33)" """
    Process p = cmd.execute();
    p.waitFor();
    if (p.exitValue() == 15) {
      assert false : "server may not be running"
    }
    assert p.exitValue() == 33
  }

}
