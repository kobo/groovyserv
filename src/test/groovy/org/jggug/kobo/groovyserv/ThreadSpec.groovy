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
class ThreadSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")

    def "using a thread"() {
        when:
        def p = TestUtils.executeClientOk(["-e", '"(new Thread({-> println(\'output from thread\') } as Runnable)).start()"'])
        p.waitFor()

        then:
        p.in.text == 'output from thread' + SEP
        p.err.text == ''
    }

    def "interruptable infinite loop in a thread"() {
        given:
        def script = """\
            Thread.start {
                println('started')
                while (true) {
                    Thread.sleep 100
                }
                println("end")
            }
            Thread.sleep 1000
            Thread.currentThread().interrupt()
        """.replaceAll(/\n/, '; ').replaceAll(/ +/, ' ').trim()

        when:
        def p = TestUtils.executeClientOk(["-e", "$script"])

        then:
        p.in.text == 'started' + SEP
        p.err.text == ''
    }

    def "uninterruptable infinite loop in a thread"() {
        given:
        def script = """\
            Thread.start {
                println('started')
                while (true) {
                    /* cannot interruptable */
                }
                println("end")
            }
            Thread.sleep 1000
            Thread.currentThread().interrupt()
        """.replaceAll(/\n/, '; ').replaceAll(/ +/, ' ').trim()

        when:
        def p = TestUtils.executeClientOk(["-e", "$script"])

        then:
        p.in.text == 'started' + SEP
        p.err.text == ''
    }
}
