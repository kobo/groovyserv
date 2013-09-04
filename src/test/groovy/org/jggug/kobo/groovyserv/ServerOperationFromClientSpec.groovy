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
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
@IgnoreIf({ properties["os.name"].startsWith("Windows") })
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ServerOperationFromClientSpec extends Specification {

    def setup() {
        TestUtils.shutdownServer()

        // just in case.
        deleteAuthTokenFile()
    }

    def cleanup() {
        TestUtils.shutdownServer()
        deleteAuthTokenFile()
    }

    def cleanupSpec() {
        // make sure server running
        TestUtils.startServer()
    }

    def "executes client with no running server"() {
        when:
        def p = executeClient()

        then:
        assertIncludingVersion(p.in.text)
        assertIncludingServerInvocationLog(p.err.text)
    }

    def "executes client with running server"() {
        given:
        TestUtils.startServer()

        when:
        def p = executeClient()

        then:
        assertIncludingVersion(p.in.text)
        assertNotIncludingServerInvocationLog(p.err.text)
    }

    def "executes client with no running server when there are previous an authtoken file"() {
        given:
        createAuthTokenFile()

        when:
        def p = executeClient()

        then:
        assertIncludingVersion(p.in.text)
        assertIncludingServerInvocationLog(p.err.text)
    }

    private static executeClient(List options = []) {
        TestUtils.executeClientOk(["-v"] + options)
    }

    private static void assertIncludingServerInvocationLog(text, restart = false) {
        assert text.contains("Invoking server")
        assert text.contains("Restarting") == restart
        assert text.contains("Starting")
        assert text.contains("is successfully started")
    }

    private static void assertNotIncludingServerInvocationLog(text) {
        assert text == ""
    }

    private static void assertIncludingVersion(text) {
        assert text.contains("Groovy Version:")
        assert text.contains("GroovyServ Version: Server")
        assert text.contains("GroovyServ Version: Client")
    }

    private static createAuthTokenFile() {
        WorkFiles.AUTHTOKEN_FILE << "DUMMY_TOKEN_FOR_TEST"
    }

    private static deleteAuthTokenFile() {
        WorkFiles.AUTHTOKEN_FILE.delete()
    }
}
