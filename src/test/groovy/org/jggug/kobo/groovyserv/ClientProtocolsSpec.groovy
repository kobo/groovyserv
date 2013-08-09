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
package org.jggug.kobo.groovyserv

import org.jggug.kobo.groovyserv.test.UnitTest
import spock.lang.Specification

/**
 * Specifications for the {@link org.jggug.kobo.groovyserv.ClientProtocols} class.
 */
@UnitTest
class ClientProtocolsSpec extends Specification {

    def "parseHeaders() for InvocationRequest having full parameters"() {
        given:
        def ins = new ByteArrayInputStream("""\
            |Cwd: /tmp/cwd
            |Cp: /tmp/cp1:/tmp/cp2
            |Arg: argument_1
            |Arg: argument_2
            |Auth: DUMMY_AUTHTOKEN
            |""".stripMargin().bytes)

        when:
        def headers = ClientProtocols.parseHeaders('ID:0', ins)

        then:
        headers.Cwd == ['/tmp/cwd']
        headers.Cp == ['/tmp/cp1:/tmp/cp2']
        headers.Arg == ['argument_1', 'argument_2']
        headers.Auth == ['DUMMY_AUTHTOKEN']
        headers.size() == 4
    }

    def "parseHeaders() for InvocationRequest having only required parameters"() {
        given:
        def ins = new ByteArrayInputStream("""\
            |Cwd: /tmp/cwd
            |Auth: DUMMY_AUTHTOKEN
            |""".stripMargin().bytes)

        when:
        def headers = ClientProtocols.parseHeaders('ID:0', ins)

        then:
        headers.Cwd == ['/tmp/cwd']
        headers.Auth == ['DUMMY_AUTHTOKEN']
        headers.size() == 2
    }

    def "parseHeaders() for normal StreamRequest"() {
        given:
        def ins = new ByteArrayInputStream("""\
            |Size: 10
            |
            |1234567890
            |""".stripMargin().bytes)

        when:
        def headers = ClientProtocols.parseHeaders('ID:0', ins)

        then:
        headers.Size == ['10']
        headers.size() == 1
    }

    def "parseHeaders() for StreamRequest as interruption"() {
        given:
        def ins = new ByteArrayInputStream("""\
            |Size: -1
            |
            |""".stripMargin().bytes)

        when:
        def headers = ClientProtocols.parseHeaders('ID:0', ins)

        then:
        headers.Size == ['-1']
        headers.size() == 1
    }

    def "parseHeaders() can trim the value"() {
        given:
        def ins = new ByteArrayInputStream("""\
            |Space:     HAS_SPACE_BEFORE_AFTER
            |Tab: \tHAS_TAB_BEFORE_AFTER\t
            |""".stripMargin().bytes)

        when:
        def headers = ClientProtocols.parseHeaders('ID:0', ins)

        then:
        headers.Space == ['HAS_SPACE_BEFORE_AFTER']
        headers.Tab == ['HAS_TAB_BEFORE_AFTER']
        headers.size() == 2
    }

    def "formatAsResponseHeader()"() {
        expect:
        ClientProtocols.formatAsResponseHeader(ch, size) == expected.bytes

        where:
        ch    | size  | expected
        'out' | 0     | 'Channel: out\nSize: 0\n\n'
        'err' | 12345 | 'Channel: err\nSize: 12345\n\n'
    }

    def "formatAsExitHeader()"() {
        expect:
        ClientProtocols.formatAsExitHeader(status) == expected.bytes

        where:
        status | expected
        0      | 'Status: 0\n\n'
        12345  | 'Status: 12345\n\n'
        -1     | 'Status: -1\n\n'
    }
}
