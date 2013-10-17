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
import org.jggug.kobo.groovyserv.test.IndependentForSpecificClient
import spock.lang.Specification

/**
 * Specifications when using socket directly
 */
@IntegrationTest
@IndependentForSpecificClient
class GroovyClientSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")
    static final int WAIT_TIME = 1000

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
        Thread.sleep(WAIT_TIME)

        then:
        client.readAllAvailable()
        client.outText == "hello" + SEP
        client.errText == ""
    }

    def "using StreamRequest"() {
        when:
        client.run('-e', 'System.in.eachLine { line, index -> println(line * 2) }')
        Thread.sleep(WAIT_TIME)

        and:
        client.input("A")
        Thread.sleep(WAIT_TIME)

        then:
        client.readAllAvailable()
        client.outText == "AA" + SEP
        client.errText == ""

        when:
        client.input("B")
        Thread.sleep(WAIT_TIME)

        then:
        client.readAllAvailable()
        client.outText == "BB" + SEP
        client.errText == ""
    }

    def "interrupted by client"() {
        when:
        client.run('-e', 'println "CCCC"; while (true) { sleep 1000 }')
        Thread.sleep(1500)

        then:
        client.readAllAvailable()
        client.outText == "CCCC" + SEP
        client.errText == ""

        when:
        Thread.sleep(WAIT_TIME)
        client.interrupt()
        Thread.sleep(WAIT_TIME)

        then:
        client.readAllAvailable()
        client.exitStatus == ExitStatus.INTERRUPTED.code
        Thread.sleep(WAIT_TIME)
    }

    def "passing environment variables"() {
        given:
        String envVarName = "TEST_ENV_NAME" + System.currentTimeMillis()
        String envVarValue = "TEST_ENV_VALUE" + System.currentTimeMillis()
        client.environments << "${envVarName}=${envVarValue}"

        when:
        client.run('-e', """println "D" + System.getenv("$envVarName") + "D";""")
        Thread.sleep(WAIT_TIME)

        then:
        client.readAllAvailable()
        client.outText == "D${envVarValue}D" + SEP
        client.errText == ""

        and:
        client.exitStatus == ExitStatus.SUCCESS.code
    }
}
