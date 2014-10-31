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

@IntegrationTest
class GroovyClientSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")
    GroovyClient client

    def setup() {
        client = new GroovyClient("localhost", 1961)
        client.connect()
    }

    def cleanup() {
        client.disconnect()
    }

    def "only InvocationRequest"() {
        when:
        client.run('-e', 'println("hello")')

        then:
        client.waitForExit()
        client.exitStatus == ExitStatus.SUCCESS.code
        client.outText == "hello" + SEP
        client.errText == ""
    }

    def "using StreamRequest"() {
        when:
        client.run('-e', 'System.in.eachLine { line, index -> println(line * 2) }')
        Thread.sleep(1000)

        and:
        client.input("A")

        then:
        client.waitForResponse()
        client.outText == "AA" + SEP
        client.errText == ""
        client.clearBuffer()

        when:
        client.input("B")

        then:
        client.waitForResponse()
        client.outText == "BB" + SEP
        client.errText == ""
    }

    def "interrupted by client"() {
        when:
        client.run('-e', 'println "CCCC"; while (true) { Thread.sleep 1000 }') // Don't use Object#sleep() in a main thread because it's not interruptable.

        then:
        client.waitForResponse()
        client.outText == "CCCC" + SEP
        client.errText == ""

        when:
        client.interrupt()

        then:
        client.waitForExit()
        client.exitStatus == ExitStatus.INTERRUPTED.code
    }

    def "passing environment variables"() {
        given:
        String envVarName = "TEST_ENV_NAME" + System.currentTimeMillis()
        String envVarValue = "TEST_ENV_VALUE" + System.currentTimeMillis()
        client.environments << "${envVarName}=${envVarValue}"

        when:
        client.run('-e', """println "D" + System.getenv("$envVarName") + "D";""")

        then:
        client.waitForExit()
        client.exitStatus == ExitStatus.SUCCESS.code
        client.outText == "D${envVarValue}D" + SEP
        client.errText == ""
    }
}
