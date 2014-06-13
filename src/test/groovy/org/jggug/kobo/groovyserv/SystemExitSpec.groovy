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
class SystemExitSpec extends Specification {

    def "using System.exit() doesn't cause a kill of server process"() {
        when:
        TestUtils.executeClientScript(['-e', '"System.exit()"'])

        and:
        def p = TestUtils.executeClientScript(['-e', '"print(\'Still There?\')"'])
        p.waitFor()

        then:
        p.in.text == "Still There?"
        p.err.text == ""
    }

    def "the value of System.exit() is returns as status code of client"() {
        expect:
        TestUtils.executeClientScript(['-e', script]).exitValue() == statusCode

        where:
        script            | statusCode
        "System.exit(1)"  | 1
        "System.exit(33)" | 33
    }
}
