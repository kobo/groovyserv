package org.jggug.kobo.groovyserv


import groovy.util.GroovyTestCase

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.Dump} class.
 * Before running this, you must start groovyserver.
 */
class ThreadTest extends GroovyTestCase {
  static final String NL = System.getProperty("line.separator");
  static final String FS = System.getProperty("file.separator");

  static executeOnServer(String script) {
	def cmd = """bin${FS}groovyclient -e "$script" """
	Process p = cmd.execute()
	p.waitFor()
	return p.getInputStream().text
  }

  void testExec() {
	assert executeOnServer("new Thread({->println 'output from thread'}as Runnable).start() ") == 'output from thread' + NL
  }

}
