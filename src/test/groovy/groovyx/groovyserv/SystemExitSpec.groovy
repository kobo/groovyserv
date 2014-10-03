/*
 * Copyright 2009 the original author or authors.
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

import groovyx.groovyserv.test.IntegrationTest
import groovyx.groovyserv.test.TestUtils
import spock.lang.Specification

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
class SystemExitSpec extends Specification {

    def "using System.exit() doesn't cause a kill of server process"() {
        when:
        assert TestUtils.executeClientCommand(['-e', '"System.exit(0)"']).process.exitValue() == 0

        and:
        def result = TestUtils.executeClientCommand(['-e', '"print(\'Still There?\')"'])

        then:
        result.out == "Still There?"
        result.err == ""
    }

    def "the value of System.exit() is returns as status code of client"() {
        when:
        def result = TestUtils.executeClientCommand(['-e', "System.exit($exitArg)"])

        then:
        result.process.exitValue() == statusCode

        where:
        exitArg | statusCode
        -2      | 254
        -1      | 255
        0       | 0
        1       | 1
        99      | 99
        255     | 255
        256     | 0
    }
}
