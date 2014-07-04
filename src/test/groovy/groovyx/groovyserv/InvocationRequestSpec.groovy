/*
 * Copyright 2009-2013 the original author or authors.
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

import groovyx.groovyserv.exception.InvalidAuthTokenException
import groovyx.groovyserv.test.UnitTest
import org.spockframework.runtime.SpockTimeoutError
import spock.lang.FailsWith
import spock.lang.Specification
import spock.lang.Timeout

@UnitTest
class InvocationRequestSpec extends Specification {

    def "client and server tokens required"() {
        given:
        def request = new InvocationRequest(
            clientAuthToken: "VALID_TOKEN",
            serverAuthToken: new AuthToken("VALID_TOKEN")
        )

        when:
        request.check()

        then:
        noExceptionThrown()
    }

    def "invalid when not matching clientAuthToken and serverAuthToken"() {
        given:
        def request = new InvocationRequest(
            clientAuthToken: clientAuthtoken,
            serverAuthToken: serverAuthToken,
        )

        and: "prevent a slow test"
        request.waitTime = 0

        when:
        request.check()

        then:
        thrown InvalidAuthTokenException

        where:
        clientAuthtoken | serverAuthToken
        "TOKEN1"        | new AuthToken("TOKEN2")
        "TOKEN1"        | null
        null            | new AuthToken("TOKEN2")
        null            | null
    }

    @FailsWith(SpockTimeoutError)
    @Timeout(3)
    def "waiting long time when check result is invalid"() {
        given:
        def request = new InvocationRequest()

        when:
        request.check()

        then:
        thrown InvalidAuthTokenException
    }
}
