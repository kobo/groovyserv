/*
 * Copyright 2009-2011 the original author or authors.
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
class ExecIT extends GroovyTestCase {

    static final String SEP = System.getProperty("line.separator")

    void testExec() {
        def p = TestUtils.executeClientOk(["-e", '"println(\'hello\')"'])
        assertEquals "hello" + SEP, p.text
        assertEquals "", p.err.text
    }

    void testMultiLineWrite() {
        def p = TestUtils.executeClientOk(["-e", '"[0, 1, 2].each{ println(it) }"'])
        assertEquals "0" + SEP + "1" + SEP + "2" + SEP, p.text
        assertEquals "", p.err.text
    }

    void testMultiLineReadAndWrite() {
        def p = TestUtils.executeClient(["-e", '"System.in.eachLine { line, index -> println(line * 2); if (index >= 2) { System.exit 0 } }"']) { p ->
            p.out << "A" + SEP
            p.out << "B" + SEP
        }
        assertEquals "AA" + SEP + "BB" + SEP, p.text
        assertEquals "", p.err.text
    }

}
