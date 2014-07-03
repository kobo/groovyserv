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
class EncodingSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")

    def "executes a file written by UTF-8"() {
        expect:
        TestUtils.executeClientCommandSuccessfully(["-c", "UTF-8", canonicalize("src/test/resources/forEncodingTest_UTF8.groovy")]).out == "あいうえお" + SEP
    }

    def "executes a file written by Shift_JIS"() {
        expect:
        TestUtils.executeClientCommandSuccessfully(["-c", "Shift_JIS", canonicalize("src/test/resources/forEncodingTest_SJIS.groovy")]).out == "あいうえお" + SEP
    }

    private static canonicalize(path) {
        new File(path).canonicalPath
    }
}
