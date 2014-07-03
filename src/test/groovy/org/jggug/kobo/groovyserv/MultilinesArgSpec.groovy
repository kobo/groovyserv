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
 * Specification for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
class MultilinesArgSpec extends Specification {

    def "can handle an argument having multi lines"() {
        given:
        def script = """\
            |print('start:')
            |print('''line1
            |line2
            |line3''')
            |print(':end')""".stripMargin()

        expect:
        TestUtils.executeClientCommandSuccessfully(["-e", "\"$script\""]).out == """\
            |start:line1
            |line2
            |line3:end""".stripMargin()
    }

}
