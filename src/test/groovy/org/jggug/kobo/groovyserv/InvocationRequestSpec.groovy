package org.jggug.kobo.groovyserv

import org.jggug.kobo.groovyserv.exception.InvalidAuthTokenException
import org.jggug.kobo.groovyserv.test.UnitTest
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
