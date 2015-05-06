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

@IntegrationTest
class EncodingSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")

    // System.out always uses default platform encoding regardless of the -c flag.
    // We must convert our test data using the default encoding as well for it to
    // always match the test output.
    static final String DATA = new String("あいうえお АБВГД".getBytes())

    def "executes a file written by UTF-8"() {
        expect:
        TestUtils.executeClientCommandSuccessfully(["-c", "UTF-8", canonicalize("src/test/resources/forEncodingTest_UTF8.groovy")]).out == DATA + SEP
    }

    def "executes a file written by Shift_JIS"() {
        expect:
        TestUtils.executeClientCommandSuccessfully(["-c", "Shift_JIS", canonicalize("src/test/resources/forEncodingTest_SJIS.groovy")]).out == DATA + SEP
    }

    private static canonicalize(path) {
        new File(path).canonicalPath
    }
}
