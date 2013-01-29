/*
 * Copyright 2009-2011 the original author or authors.
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

/**
 * Tests for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
class VersionIT extends GroovyTestCase {

    static final String SEP = System.getProperty("line.separator")

    void testVersionOption_clientAndServer_short() {
        def p = TestUtils.executeClientOk(["-v"])
        p.in.text.with { stdout ->
            assert stdout.contains("Groovy Version:")
            assert stdout.contains("GroovyServ Version: Server")
            assert stdout.contains("GroovyServ Version: Client")
        }
        assert p.err.text == ""
    }

    void testVersionOption_clientAndServer_vagueShort() {
        // the original Groovy considers the option value to a valid version option.
        def p = TestUtils.executeClientOk(["-vFOOBAR"])
        p.in.text.with { stdout ->
            assert stdout.contains("Groovy Version:")
            assert stdout.contains("GroovyServ Version: Server")
            assert stdout.contains("GroovyServ Version: Client")
        }
        assert p.err.text == ""
    }

    void testVersionOption_clientAndServer_long() {
        def p = TestUtils.executeClientOk(["--version"])
        p.in.text.with { stdout ->
            assert stdout.contains("Groovy Version:")
            assert stdout.contains("GroovyServ Version: Server")
            assert stdout.contains("GroovyServ Version: Client")
        }
        assert p.err.text == ""
    }

    void testVersionOption_onlyClient_short() {
        def p = TestUtils.executeClientOk(["-Cv"])
        p.in.text.with { stdout ->
            assert stdout.contains("Groovy Version:") == false
            assert stdout.contains("GroovyServ Version: Server") == false
            assert stdout.contains("GroovyServ Version: Client")
        }
        assert p.err.text == ""
    }

    void testVersionOption_onlyClient_long() {
        def p = TestUtils.executeClientOk(["-Cversion"])
        p.in.text.with { stdout ->
            assert stdout.contains("Groovy Version:") == false
            assert stdout.contains("GroovyServ Version: Server") == false
            assert stdout.contains("GroovyServ Version: Client")
        }
        assert p.err.text == ""
    }

}
