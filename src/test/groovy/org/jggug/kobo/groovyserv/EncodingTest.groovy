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

// if this test fails, please try
// export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
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
