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

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
class VersionSpec extends Specification {

    def "'-v' option prints versions both of client and server"() {
        when:
        def p = TestUtils.executeClientOk(["-v"])

        then:
        def stdout = p.in.text
        stdout.contains("Groovy Version:")
        stdout.contains("GroovyServ Version: Server")
        stdout.contains("GroovyServ Version: Client")
        p.err.text == ""
    }

    def "'-vXxxxx' option prints versions both of client and server"() {
        when:
        // the original Groovy considers the option value to a valid version option.
        def p = TestUtils.executeClientOk(["-vFOOBAR"])

        then:
        def stdout = p.in.text
        stdout.contains("Groovy Version:")
        stdout.contains("GroovyServ Version: Server")
        stdout.contains("GroovyServ Version: Client")
        p.err.text == ""
    }

    def "'--version' option prints versions both of client and server"() {
        when:
        def p = TestUtils.executeClientOk(["--version"])

        then:
        def stdout = p.in.text
        stdout.contains("Groovy Version:")
        stdout.contains("GroovyServ Version: Server")
        stdout.contains("GroovyServ Version: Client")
        p.err.text == ""
    }

    def "'-Cv' option prints only client version"() {
        when:
        def p = TestUtils.executeClientOk(["-Cv"])

        then:
        def stdout = p.in.text
        stdout.contains("Groovy Version:") == false
        stdout.contains("GroovyServ Version: Server") == false
        stdout.contains("GroovyServ Version: Client")
        p.err.text == ""
    }

    def "'-Cversion' option prints only client version"() {
        when:
        def p = TestUtils.executeClientOk(["-Cversion"])

        then:
        def stdout = p.in.text
        stdout.contains("Groovy Version:") == false
        stdout.contains("GroovyServ Version: Server") == false
        stdout.contains("GroovyServ Version: Client")
        p.err.text == ""
    }

}
