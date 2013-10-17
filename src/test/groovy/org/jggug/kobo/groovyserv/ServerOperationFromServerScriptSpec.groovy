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
@IndependentForSpecificClient
class ServerOperationFromServerScriptSpec extends Specification {

    def cleanupSpec() {
        TestUtils.startServerIfNotRunning()
    }

    def "try to start server with no running server (when there are no authtoken file)"() {
        given:
        TestUtils.shutdownServerIfRunning()

        when:
        def p = TestUtils.executeServerScript([])

        then:
        assertNeverUseStandardInputStream(p)
        p.err.text=~/Server is successfully started up on [0-9]+ port/
    }

    def "try to start server with no running server when there are a previous authtoken file"() {
        given:
        TestUtils.shutdownServerIfRunning()
        createAuthTokenFile()

        when:
        def p = TestUtils.executeServerScript([])

        then:
        assertNeverUseStandardInputStream(p)
        with(p.err.text) {
            it=~/WARN: authtoken file exists: .*authtoken-[0-9]+ \(overwritten by new authtoken\)/
            it=~/Server is successfully started up on [0-9]+ port/
        }
    }

    def "try to start server with running server (when there are an authtoken file)"() {
        given:
        TestUtils.startServerIfNotRunning()

        when:
        def p = TestUtils.executeServerScript([])

        then:
        assertNeverUseStandardInputStream(p)
        p.err.text=~/WARN: server is already running on [0-9]+ port/
    }

    def "try to start server with running server when there are an invalid authtoken file"() {
        given:
        TestUtils.startServerIfNotRunning()
        def originalToken = updateAuthTokenFile("INVALID_TOKEN")

        when:
        def p = TestUtils.executeServerScript([])

        then:
        assertNeverUseStandardInputStream(p)
        with(p.err.text) {
            it=~/WARN: server is already running on [0-9]+ port/
            !it.contains("ERROR:")
        }

        cleanup:
        updateAuthTokenFile(originalToken)
    }

    def "try to start server with running server when there are no authtoken file"() {
        given:
        TestUtils.startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()

        when:
        def p = TestUtils.executeServerScript([])

        then:
        assertNeverUseStandardInputStream(p)
        with(p.err.text) {
            it=~/WARN: server is already running on [0-9]+ port/
            it=~/ERROR: could not find authtoken file: /
            it=~/Hint:  Isn't the port being used by a non-groovyserv process\? If not so, please kill the process somehow\./
        }

        cleanup:
        createAuthTokenFile(originalToken)
    }

    def "try to kill server with no running server (when there are no authtoken file)"() {
        given:
        TestUtils.shutdownServerIfRunning()

        when:
        def p = TestUtils.executeServerScript(["-k"])

        then:
        assertNeverUseStandardInputStream(p)
        p.err.text=~/WARN: server is not running/
    }

    def "try to kill server with no running server when there are a previous authtoken file"() {
        given:
        TestUtils.shutdownServerIfRunning()
        createAuthTokenFile()

        when:
        def p = TestUtils.executeServerScript(["-k"])

        then:
        assertNeverUseStandardInputStream(p)
        with(p.err.text) {
            it=~/WARN: server is not running/
            it=~/WARN: previous authtoken file is deleted: /
        }
        !WorkFiles.AUTHTOKEN_FILE.exists()
    }

    def "try to kill server with running server (when there are an authtoken file)"() {
        given:
        TestUtils.startServerIfNotRunning()

        when:
        def p = TestUtils.executeServerScript(["-k"])

        then:
        assertNeverUseStandardInputStream(p)
        p.err.text=~/Server is successfully shut down/
    }

    def "try to kill server with running server when there are an invalid authtoken file"() {
        given:
        TestUtils.startServerIfNotRunning()
        def originalToken = updateAuthTokenFile("INVALID_TOKEN")

        when:
        def p = TestUtils.executeServerScript(["-k"])

        then:
        assertNeverUseStandardInputStream(p)
        with(p.err.text) {
            it=~/ERROR: invalid authtoken/
            it=~/Hint:  Please specify a right authtoken or just kill the process somehow./
        }

        cleanup:
        updateAuthTokenFile(originalToken)
    }

    def "try to kill server with running server when there are no authtoken file"() {
        given:
        TestUtils.startServerIfNotRunning()
        def originalToken = deleteAuthTokenFile()

        when:
        def p = TestUtils.executeServerScript(["-k"])

        then:
        assertNeverUseStandardInputStream(p)
        with(p.err.text) {
            it=~/ERROR: could not find authtoken file: /
            it=~/Hint:  Please kill the process somehow./
        }

        cleanup:
        createAuthTokenFile(originalToken)
    }

    private static void assertNeverUseStandardInputStream(Process p) {
        assert p.in.text == "" // server script doesn't use standard input stream
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
