/*
 * Copyright 2009-2013 the original author or authors.
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

import org.jggug.kobo.groovyserv.test.IntegrationTest
import org.jggug.kobo.groovyserv.test.TestUtils
import spock.lang.Specification

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
class ExecScriptSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")

    def "executes an simple one-liner"() {
        when:
        def p = TestUtils.executeClientOk(["-e", '"println(\'hello\')"'])

        then:
        p.in.text == "hello" + SEP
        p.err.text == ""
    }

    def "executes an one-liner which prints multi lines"() {
        when:
        def p = TestUtils.executeClientOk(["-e", '"[0, 1, 2].each{ println(it) }"'])

        then:
        p.in.text == "0" + SEP + "1" + SEP + "2" + SEP
        p.err.text == ""
    }

    def "executes an one-liner which reads and prints multi lines"() {
        when:
        def p = TestUtils.executeClient(["-e", '"System.in.eachLine { line, index -> println(line * 2); if (index >= 3) { System.exit 0 } }"']) { p ->
            // improved to be able to detect the issue: https://github.com/kobo/groovyserv/issues/44
            // when two lines sequentially are inputted, it exists normally.
            // but if inputting three lines or with a interval, c client was frozen.
            p.out << "A" + SEP
            sleep 1000
            p.out << "B" + SEP
            sleep 1000
            p.out << "C" + SEP
        }

        then:
        p.in.text == "AA" + SEP + "BB" + SEP + "CC" + SEP
        p.err.text == ""
    }

}
