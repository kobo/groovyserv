/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    def cmd = """bin${FS}groovyclient -e "println('hello')" """
    Process p = cmd.execute()
    p.waitFor()
    if (p.exitValue() == 201) {
      assert false : "server may not be running"
    }

    assert p.getInputStream().text == "hello"+NL
    p.getErrorStream().eachLine {
      if (it == 'connect: Connection refused') {
        assert false : "server may not be running"
      }
      assert false: "error output returned from the server: "+it
    }

  }

  void testMultiLineWrite() {
    def cmd = """bin${FS}groovyclient -e "[0,1,2].each{println(it)}" """
    Process p = cmd.execute()
    p.waitFor()
    if (p.exitValue() == 201) {
      assert false : "server may not be running"
    }
    assert p.getInputStream().text == "0"+NL+"1"+NL+"2"+NL
    p.getErrorStream().eachLine {
      if (it == 'connect: Connection refused') {
        assert false : "server may not running"
      }
      assert false: "error output returned from server: "+it
    }
  }

  void testMultiLineRead() {
    def cmd = """bin${FS}groovyclient -e "System.in.eachLine{println(it+it)}" """
    Process p = cmd.execute()
    def os =  p.getOutputStream()
    os.write("A${NL}B${NL}".getBytes())
    os.close()

    p.waitFor()
    if (p.exitValue() == 201) {
      assert false : "server may not be running"
    }

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
