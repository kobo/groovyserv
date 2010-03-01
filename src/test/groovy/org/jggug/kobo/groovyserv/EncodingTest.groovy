package org.jggug.kobo.groovyserv;


import groovy.util.GroovyTestCase

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.Dump} class.
 * Before running this, you must start groovyserver.
 */
class EncodingTest extends GroovyTestCase {
  static final String NL = System.getProperty("line.separator");
  static final String FS = System.getProperty("file.separator");

  static executeClient(String line) {
	  def cmd = """bin${FS}groovyclient $line """;
	  Process p = cmd.execute();
	  p.waitFor();
	  return p.getInputStream().text
  }

  static executeClientScript(String script) {
	  return executeClient("""-e "$script" """);
  }

  void testExec() {
	  assert executeClientScript("println 'あいうえお'") == "あいうえお" + NL
	  assert executeClient("src/test/groovy/org/jggug/kobo/groovyserv/enchelper.groovy") == "あいうえお" + NL
  }

}
