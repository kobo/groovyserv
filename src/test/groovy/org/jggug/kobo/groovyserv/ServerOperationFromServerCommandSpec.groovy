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
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ServerOperationFromServerCommandSpec extends Specification {

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

    def "try to start server with no running server (when there are no authtoken file)"() {
        given:
        shutdownServerIfRunning()

        when:
        def result = executeServerCommand()

        then:
        result.out == ""
        result.err =~ /Server is successfully started up on [0-9]+ port/
    }

    def "try to start server with no running server when there are a previous authtoken file"() {
        given:
        shutdownServerIfRunning()
        createAuthTokenFile()

        when:
        def result = executeServerCommand()

        then:
        result.out == ""
        with(result.err) {
            result.err =~ /WARN: old authtoken file is deleted: /
            result.err =~ /Server is successfully started up on [0-9]+ port/
        }
    }

    def "try to start server with running server (when there are an authtoken file)"() {
        given:
        startServerIfNotRunning()

        when:
        def result = executeServerCommand()

        then:
        result.out == ""
        result.err =~ /WARN: server is already running on [0-9]+ port/
    }

    def "try to start server with running server when there are an invalid authtoken file"() {
        given:
        startServerIfNotRunning()
        def originalToken = updateAuthTokenFile("INVALID_TOKEN")

        when:
        def result = executeServerCommand()

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: invalid authtoken/
            it =~ /Hint:  Specify a right authtoken or kill the process somehow\./
        }

        cleanup:
        updateAuthTokenFile(originalToken)
    }

    def "try to start server with running server when there are no authtoken file"() {
        given:
        startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()

        when:
        def result = executeServerCommand()

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: could not open authtoken file: /
            it =~ /Hint:  Isn't the port being used by a non-groovyserv process\? Use another port or kill the process somehow\./
        }

        cleanup:
        createAuthTokenFile(originalToken)
    }

    def "try to start server with running server when authtoken file could not read as file"() {
        given:
        startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()
        WorkFiles.AUTHTOKEN_FILE.mkdir() // not readable as file

        when:
        def result = executeServerCommand()

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: could not read authtoken file: /
            it =~ /Hint:  Check the permission and file type\./
        }

        cleanup:
        WorkFiles.AUTHTOKEN_FILE.deleteDir()
        createAuthTokenFile(originalToken)
    }

    def "try to kill server with no running server (when there are no authtoken file)"() {
        given:
        shutdownServerIfRunning()

        when:
        def result = executeServerCommand(["-k"])

        then:
        result.out == ""
        result.err =~ /WARN: server is not running/
    }

    def "try to kill server with no running server when there are a previous authtoken file"() {
        given:
        shutdownServerIfRunning()
        createAuthTokenFile()

        when:
        def result = executeServerCommand(["-k"])

        then:
        result.out == ""
        with(result.err) {
            it =~ /WARN: server is not running/
            it =~ /WARN: old authtoken file is deleted: /
        }
        !WorkFiles.AUTHTOKEN_FILE.exists()
    }

    def "try to kill server with running server (when there are an authtoken file)"() {
        given:
        startServerIfNotRunning()

        when:
        def result = executeServerCommand(["-k"])

        then:
        result.out == ""
        result.err =~ /Server is successfully shut down/
    }

    def "try to kill server with running server when there are an invalid authtoken file"() {
        given:
        startServerIfNotRunning()
        def originalToken = updateAuthTokenFile("INVALID_TOKEN")

        when:
        def result = executeServerCommand(["-k"])

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: invalid authtoken/
            it =~ /Hint:  Specify a right authtoken or kill the process somehow\./
        }

        cleanup:
        updateAuthTokenFile(originalToken)
    }

    def "try to kill server with running server when there are no authtoken file"() {
        given:
        startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()

        when:
        def result = executeServerCommand(["-k"])

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: could not open authtoken file: /
            it =~ /Hint:  Isn't the port being used by a non-groovyserv process\? Use another port or kill the process somehow\./
        }

        cleanup:
        createAuthTokenFile(originalToken)
    }

    def "try to kill server with running server when authtoken file could not read as file"() {
        given:
        startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()
        WorkFiles.AUTHTOKEN_FILE.mkdir() // not readable as file

        when:
        def result = executeServerCommand(["-k"])

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: could not read authtoken file: /
            it =~ /Hint:  Check the permission and file type\./
        }

        cleanup:
        WorkFiles.AUTHTOKEN_FILE.deleteDir()
        createAuthTokenFile(originalToken)
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

    private static executeServerCommand(List<String> options = []) {
        return TestUtils.executeServerCommand(options + ["-p", port])
    }
}
