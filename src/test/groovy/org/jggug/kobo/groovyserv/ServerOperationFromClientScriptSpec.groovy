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
class ServerOperationFromClientScriptSpec extends Specification {

    def "try to execute client with no running server (when there are no authtoken file)"() {
        given:
        TestUtils.shutdownServerIfRunning()

        when:
        def p = TestUtils.executeClientScriptOk(["-v"])

        then:
        assertIncludingVersion(p.in.text)
        assertIncludingServerInvocationLog(p.err.text)
    }

    def "try to execute client with no running server when there are previous an authtoken file"() {
        given:
        TestUtils.shutdownServerIfRunning()
        createAuthTokenFile()

        when:
        def p = TestUtils.executeClientScriptOk(["-v"])

        then:
        assertIncludingVersion(p.in.text)
        assertIncludingServerInvocationLog(p.err.text)
    }

    def "try to execute client with running server (when there are an authtoken file)"() {
        given:
        TestUtils.startServerIfNotRunning()

        when:
        def p = TestUtils.executeClientScriptOk(["-v"])

        then:
        assertIncludingVersion(p.in.text)
        assertNotIncludingServerInvocationLog(p.err.text)
    }

    def "try to execute client with running server when there are an invalid authtoken file"() {
        given:
        TestUtils.startServerIfNotRunning()
        def originalToken = updateAuthTokenFile("INVALID_TOKEN")

        when:
        def p = TestUtils.executeClientScript(["-v"])

        then:
        p.err.text =~ /ERROR: invalid authtoken/

        cleanup:
        updateAuthTokenFile(originalToken)
    }

    def "try to execute client with running server when there are no authtoken file"() {
        given:
        TestUtils.startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()

        when:
        def p = TestUtils.executeClientScript(["-v"])

        then:
        p.err.text =~ /ERROR: could not read authtoken file: /

        cleanup:
        createAuthTokenFile(originalToken)
    }

    def "try to execute client with running server when there are no permission for an authtoken file"() {
        given:
        TestUtils.startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()
        WorkFiles.AUTHTOKEN_FILE.mkdir() // to fail to read as file

        when:
        def p = TestUtils.executeClientScript(["-v"])

        then:
        p.err.text =~ /ERROR: could not read authtoken file: /

        cleanup:
        WorkFiles.AUTHTOKEN_FILE.deleteDir()
        createAuthTokenFile(originalToken)
    }

    private static void assertIncludingServerInvocationLog(text) {
        assert text.contains("Start server: ")
        assert text.contains("Starting server")
        assert text.contains("Server is successfully started up")
    }

    private static void assertNotIncludingServerInvocationLog(text) {
        assert text == ""
    }

    private static void assertIncludingVersion(text) {
        assert text.contains("Groovy Version:")
        assert text.contains("GroovyServ Version: Server")
        assert text.contains("GroovyServ Version: Client")
    }

    private static void createAuthTokenFile(token = "DUMMY_TOKEN_FOR_TEST") {
        assert !WorkFiles.AUTHTOKEN_FILE.exists()
        WorkFiles.AUTHTOKEN_FILE.text = token
    }

    private static String updateAuthTokenFile(String newToken) {
        assert WorkFiles.AUTHTOKEN_FILE.exists()
        def oldToken = WorkFiles.AUTHTOKEN_FILE.text
        WorkFiles.AUTHTOKEN_FILE.text = newToken
        return oldToken
    }

    private static String deleteAuthTokenFile() {
        assert WorkFiles.AUTHTOKEN_FILE.exists()
        def oldToken = WorkFiles.AUTHTOKEN_FILE.text
        WorkFiles.AUTHTOKEN_FILE.delete()
        return oldToken
    }
}
