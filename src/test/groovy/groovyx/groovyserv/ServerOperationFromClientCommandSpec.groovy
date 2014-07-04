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
package groovyx.groovyserv

import groovyx.groovyserv.test.IntegrationTest
import groovyx.groovyserv.test.TestUtils
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ServerOperationFromClientCommandSpec extends Specification {

    static int port = GroovyServer.DEFAULT_PORT

    def setup() {
        // using an individual port for each test case.
        port++
        WorkFiles.setUp(port)
    }

    def cleanup() {
        shutdownServerIfRunning()
    }

    def cleanupSpec() {
        port = GroovyServer.DEFAULT_PORT
        WorkFiles.setUp(port)
    }

    def "try to execute client with no running server (when there are no authtoken file)"() {
        given:
        shutdownServerIfRunning()

        when:
        def result = executeClientCommandSuccessfully()

        then:
        assertIncludingVersion(result.out)
        assertIncludingServerInvocationLog(result.err)
    }

    def "try to execute client with no running server when there are previous an authtoken file"() {
        given:
        shutdownServerIfRunning()
        createAuthTokenFile()

        when:
        def result = executeClientCommandSuccessfully()

        then:
        assertIncludingVersion(result.out)
        assertIncludingServerInvocationLog(result.err)
    }

    def "try to execute client with running server (when there are an authtoken file)"() {
        given:
        startServerIfNotRunning()

        when:
        def result = executeClientCommandSuccessfully()

        then:
        assertIncludingVersion(result.out)
        assertNotIncludingServerInvocationLog(result.err)
    }

    def "try to execute client with running server when there are an invalid authtoken file"() {
        given:
        startServerIfNotRunning()
        def originalToken = updateAuthTokenFile("INVALID_TOKEN")

        when:
        def result = executeClientCommand(["-v"])

        then:
        result.err =~ /ERROR: invalid authtoken/

        cleanup:
        updateAuthTokenFile(originalToken)
    }

    def "try to execute client with running server when there are no authtoken file"() {
        given:
        startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()

        when:
        def result = executeClientCommand(["-v"])

        then:
        result.err =~ /ERROR: could not read authtoken file: /

        cleanup:
        createAuthTokenFile(originalToken)
    }

    def "try to execute client with running server when there are no permission for an authtoken file"() {
        given:
        startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()
        WorkFiles.AUTHTOKEN_FILE.mkdir() // to fail to read as file

        when:
        def result = executeClientCommand()

        then:
        result.err =~ /ERROR: could not read authtoken file: /

        cleanup:
        WorkFiles.AUTHTOKEN_FILE.deleteDir()
        createAuthTokenFile(originalToken)
    }

    private static void assertIncludingServerInvocationLog(text) {
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

    private static startServerIfNotRunning() {
        TestUtils.startServerIfNotRunning(port)
    }

    private static shutdownServerIfRunning() {
        TestUtils.shutdownServerIfRunning(port)
    }

    private static executeClientCommandSuccessfully(List<String> options = []) {
        return TestUtils.executeClientCommandSuccessfully(options + ["-v", "-Cp", port])
    }

    private static executeClientCommand(List<String> options = []) {
        return TestUtils.executeClientCommand(options + ["-v", "-Cp", port])
    }
}
