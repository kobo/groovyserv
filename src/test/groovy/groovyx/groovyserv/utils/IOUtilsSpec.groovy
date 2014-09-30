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
package groovyx.groovyserv.utils

import groovyx.groovyserv.test.UnitTest
import spock.lang.Specification

/**
 * Specifications for the {@link groovyx.groovyserv.utils.IOUtils} class.
 */
@UnitTest
class IOUtilsSpec extends Specification {

    InputStream inputStream

    def setup() {
        inputStream = new ByteArrayInputStream("AAA\nBBB\nCCC\n\n".bytes)
    }

    def "readLines() returns a list of lines read from available data at input stream"() {
        when:
        def lines = IOUtils.readLines(inputStream)

        then:
        lines == ["AAA", "BBB", "CCC"]
    }

    def "readLine() returns a line read from available data or an empty if at the end of stream"() {
        expect: "retrieves a line one by one"
        IOUtils.readLine(inputStream) == "AAA"
        IOUtils.readLine(inputStream) == "BBB"
        IOUtils.readLine(inputStream) == "CCC"

        and: "just returns an empty at the end of input stream"
        IOUtils.readLine(inputStream) == ""
        IOUtils.readLine(inputStream) == ""
        IOUtils.readLine(inputStream) == ""
    }

}
