/*
 * Copyright 2009 the original author or authors.
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
class ExitTest extends GroovyTestCase {
  static final String NL = System.getProperty("line.separator");
  static final String FS = System.getProperty("file.separator");

  void testExit1() {
    def cmd = """bin${FS}groovyclient -e "System.exit(1)" """
    Process p = cmd.execute();
    p.waitFor();
    if (p.exitValue() == 15) {
      assert false : "server may not be running"
    }
    assert p.exitValue() == 1
  }

  void testExit33() {
    def cmd = """bin${FS}groovyclient -e "System.exit(33)" """
    Process p = cmd.execute();
    p.waitFor();
    if (p.exitValue() == 15) {
      assert false : "server may not be running"
    }
    assert p.exitValue() == 33
  }

}
