/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
 * Tests for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
class ExecTest extends GroovyTestCase {

    static final String NL = System.getProperty("line.separator")

    void testExec() {
        def p = TestUtils.execute(["groovyclient", "-e", '"println(\'hello\')"'])
        assertEquals "hello" + NL, p.text
        assertEquals "", p.err.text
    }

    void testMultiLineWrite() {
        def p = TestUtils.execute(["groovyclient", "-e", '"[0,1,2].each{println(it)}"'])
        assertEquals "0"+NL+"1"+NL+"2"+NL, p.text
        assertEquals "", p.err.text
    }

    void testMultiLineReadAndWrite() {
        def p = TestUtils.execute(["groovyclient", "-e", '"System.in.eachLine{println(it+it)}"']) { p ->
            p.out << "A${NL}B${NL}"
            p.out.close()
        }
        p.waitFor()
        if (p.exitValue() != 0) {
            fail "ERROR: in:[${p.in.text}], err:[${p.err.text}]"
        }
        assertEquals "AA"+NL+"BB"+NL, p.text
        assertEquals "", p.err.text
    }

}
