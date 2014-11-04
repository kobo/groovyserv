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
package groovyx.groovyserv

import groovyx.groovyserv.test.IntegrationTest
import spock.lang.Specification

/**
 * NOTE: A main thread of a user script must be composed of interruptable code to succeed in interruption.
 * Otherwise, you couldn't stop it and the thread would keep working in background infinitely.
 * JDK's {@code Thread.sleep()} is interruptable. But GDK's {@code Object#sleep()} isn't interruptable.
 */
@IntegrationTest
class ClientInterruptionSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")

    GroovyClient client

    def setup() {
        client = new GroovyClient("localhost", 1961).connect()
    }

    def "interruptable only main thread"() {
        expect:
        assertInterruptable """\
            |println('started')
            |while (true) { Thread.sleep 1000 }
            |"""
    }

    def "interruptable sub thread with waiting on main thread"() {
        expect:
        assertInterruptable """\
            |Thread.start {
            |    println('started')
            |    while (true) {
            |        sleep 1000
            |    }
            |    println("end")
            |}
            |while (true) { Thread.sleep 1000 }
            |"""
    }

    def "interruptable sub thread without waiting on main thread"() {
        expect:
        assertInterruptable """\
            |Thread.start {
            |    println('started')
            |    while (true) {
            |        sleep 1000
            |    }
            |    println("end")
            |}
            |"""
    }

    def "non-interruptable only main thread"() {
        expect:
        assertInterruptable """\
            |println('started')
            |while (true) { Thread.sleep 1000 }
            |"""
    }

    def "non-interruptable sub thread"() {
        expect:
        assertInterruptable """\
            |Thread.start {
            |    println('started')
            |    while (true) {
            |        sleep 1000
            |    }
            |    println("end")
            |}
            |"""
    }

    void assertInterruptable(script) {
        //given:
        def formattedScript = script.stripMargin().replaceAll(/\n/, '; ').replaceAll(/ +/, ' ').trim()
        client.run("-e", formattedScript)
        Thread.sleep 1000

        //when:
        client.interrupt()
        Thread.sleep 500

        //then:
        client.waitForExit()
        assert client.exitStatus == ExitStatus.INTERRUPTED.code
        assert client.outText == 'started' + SEP
        assert client.errText == ''
    }
}
