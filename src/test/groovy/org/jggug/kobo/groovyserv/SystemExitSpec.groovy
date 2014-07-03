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
        assert TestUtils.executeClientCommand(['-e', '"System.exit()"']).process.exitValue() == 0

        and:
        def result = TestUtils.executeClientCommand(['-e', '"print(\'Still There?\')"'])

        then:
        result.out == "Still There?"
        result.err == ""
    }

    def "the value of System.exit() is returns as status code of client"() {
        when:
        def result = TestUtils.executeClientCommand(['-e', script])

        then:
        result.process.exitValue() == statusCode

        where:
        script            | statusCode
        "System.exit(1)"  | 1
        "System.exit(33)" | 33
    }
}
