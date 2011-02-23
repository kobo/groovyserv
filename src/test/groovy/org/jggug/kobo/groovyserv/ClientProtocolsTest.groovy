/*
 * Copyright 2009-2010 the original author or authors.
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

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.ClientProtocols} class.
 */
class ClientProtocolsTest extends GroovyTestCase {

    void testParseHeaders_InvocationRequest_Full() {
        // setup
        def ins = new ByteArrayInputStream("""\
Cwd: /tmp/cwd
Cp: /tmp/cp1:/tmp/cp2
Arg: -e
Arg: "pritln('hello')"
Cookie: DUMMY_COOKIE
""".bytes)
        // exercise
        def headers = ClientProtocols.parseHeaders('ID:0', ins)
        // verify
        assert headers.Cwd == ['/tmp/cwd']
        assert headers.Cp == ['/tmp/cp1:/tmp/cp2']
        assert headers.Arg == ['-e', '''"pritln('hello')"''']
        assert headers.Cookie == ['DUMMY_COOKIE']
        assert headers.size() == 4
    }

    void testParseHeaders_InvocationRequest_OnlyRequired() {
        // setup
        def ins = new ByteArrayInputStream("""\
Cwd: /tmp/cwd
Cookie: DUMMY_COOKIE
""".bytes)
        // exercise
        def headers = ClientProtocols.parseHeaders('ID:0', ins)
        // verify
        assert headers.Cwd == ['/tmp/cwd']
        assert headers.Cookie == ['DUMMY_COOKIE']
        assert headers.size() == 2
    }

    void testParseHeaders_StreamRequest_Normal() {
        // setup
        def ins = new ByteArrayInputStream("""\
Size: 10

1234567890
""".bytes)
        // exercise
        def headers = ClientProtocols.parseHeaders('ID:0', ins)
        // verify
        assert headers.Size == ['10']
        assert headers.size() == 1
    }

    void testParseHeaders_StreamRequest_Interrupt() {
        // setup
        def ins = new ByteArrayInputStream("""\
Size: -1

""".bytes)
        // exercise
        def headers = ClientProtocols.parseHeaders('ID:0', ins)
        // verify
        assert headers.Size == ['-1']
        assert headers.size() == 1
    }

    void testParseHeaders_Trimmed() {
        // setup
        def ins = new ByteArrayInputStream("""\
Space:     HAS_SPACE_BEFORE_AFTER      
Tab: \tHAS_TAB_BEFORE_AFTER\t
""".bytes)
        // exercise
        def headers = ClientProtocols.parseHeaders('ID:0', ins)
        // verify
        assert headers.Space == ['HAS_SPACE_BEFORE_AFTER']
        assert headers.Tab == ['HAS_TAB_BEFORE_AFTER']
        assert headers.size() == 2
    }

    void testFormatAsResponseHeader_out_0() {
        assert ClientProtocols.formatAsResponseHeader('out', 0) == 'Channel: out\nSize: 0\n\n'.bytes
    }

    void testFormatAsResponseHeader_err_12345() {
        assert ClientProtocols.formatAsResponseHeader('err', 12345) == 'Channel: err\nSize: 12345\n\n'.bytes
    }

    void testFormatAsExitHeader_0() {
        assert ClientProtocols.formatAsExitHeader(0) == 'Status: 0\n\n'.bytes
    }

    void testFormatAsExitHeader_12345() {
        assert ClientProtocols.formatAsExitHeader(12345) == 'Status: 12345\n\n'.bytes
    }

    void testFormatAsExitHeader_minus1() {
        assert ClientProtocols.formatAsExitHeader(-1) == 'Status: -1\n\n'.bytes
    }

}
