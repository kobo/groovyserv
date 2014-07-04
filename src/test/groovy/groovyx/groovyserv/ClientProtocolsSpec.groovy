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

import groovyx.groovyserv.test.UnitTest
import spock.lang.Specification

/**
 * Specifications for the {@link groovyx.groovyserv.ClientProtocols} class.
 */
@UnitTest
class ClientProtocolsSpec extends Specification {

    def socket
    def serverAuthtoken
    def connection

    def setup() {
        socket = Mock(Socket)
        serverAuthtoken = new AuthToken("DUMMY_AUTHTOKEN")
        connection = new ClientConnection(serverAuthtoken, socket)
    }

    def "readInvocationRequest()"() {
        given:
        socket.inputStream >> new ByteArrayInputStream("""\
            |Cwd: /tmp/cwd
            |Cp: /tmp/cp1:/tmp/cp2
            |Env: FOO=foo
            |Env: BAR=bar
            |Auth: DUMMY_AUTHTOKEN
            |Arg: ${'argument_1'.bytes.encodeBase64()}
            |Arg: ${'argument_2'.bytes.encodeBase64()}
            |""".stripMargin().replaceAll(/\r/, '').bytes)

        when:
        def request = ClientProtocols.readInvocationRequest(connection)

        then:
        request.cwd == '/tmp/cwd'
        request.classpath == '/tmp/cp1:/tmp/cp2'
        request.envVars == ['FOO=foo', 'BAR=bar']
        request.clientAuthToken == 'DUMMY_AUTHTOKEN'
        request.args == ['argument_1', 'argument_2']
    }

    def "readStreamRequest()"() {
        given:
        socket.inputStream >> new ByteArrayInputStream("""\
            |Size: 19
            |
            |LINE1
            |LINE2
            |LINE3
            |""".stripMargin().replaceAll(/\r/, '').bytes)
        socket.port >> 8888

        when:
        def request = ClientProtocols.readStreamRequest(connection)

        then:
        request.port == 8888
        request.size == 19
    }

    def "readInvocationRequest() for CommandRequest of shutdown"() {
        given:
        def socket = Mock(Socket)
        socket.inputStream >> new ByteArrayInputStream("""\
            |Cmd: shutdown
            |Auth: DUMMY_AUTHTOKEN
            |""".stripMargin().bytes)
        def serverAuthtoken = new AuthToken("DUMMY_AUTHTOKEN")
        def connection = new ClientConnection(serverAuthtoken, socket)

        when:
        def request = ClientProtocols.readInvocationRequest(connection)

        then:
        request.command == 'shutdown'
        request.clientAuthToken == 'DUMMY_AUTHTOKEN'
    }

    def "readStreamRequest() for CommandRequest of interrupt"() {
        given:
        socket.inputStream >> new ByteArrayInputStream("""\
            |Cmd: interrupt
            |""".stripMargin().replaceAll(/\r/, '').bytes)
        socket.port >> 8888

        when:
        def request = ClientProtocols.readStreamRequest(connection)

        then:
        request.port == 8888
        request.size == 0
        request.command == 'interrupt'
    }

    def "readHeaders()"() {
        given:
        socket.inputStream >> new ByteArrayInputStream("""\
            |KeyA: ValueA
            |KeyB: ValueB-1
            |KeyB: ValueB-2
            |KeyB: ValueB-3
            |Key-C: Value-C
            |Key D: Value D
            |Key E E E: Value E: including colon
            |Key with empty value:
            |Key without colon
            |Key with Space:     HAS_SPACE_BEFORE_AFTER
            |Key with Tab: \tHAS_TAB_BEFORE_AFTER\t
            |
            |Key X: Here is body part because after a blank line
            |""".stripMargin().replaceAll(/\r/, '').bytes)

        when:
        def headers = ClientProtocols.readHeaders(connection)

        then:
        headers.size() == 9
        headers.KeyA == ['ValueA']
        headers.KeyB == ['ValueB-1', 'ValueB-2', 'ValueB-3']
        headers['Key-C'] == ['Value-C']
        headers['Key D'] == ['Value D']
        headers['Key E E E'] == ['Value E: including colon']
        headers['Key with empty value'] == ['']
        headers['Key without colon'] == ['']
        headers['Key with Space'] == ['HAS_SPACE_BEFORE_AFTER']
        headers['Key with Tab'] == ['HAS_TAB_BEFORE_AFTER']
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
