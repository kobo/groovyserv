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

/**
 * Tests for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
class EncodingIT extends GroovyTestCase {

    static final String SEP = System.getProperty("line.separator")

// FIXME When running with a ruby client in my Windows PC, this fails every time.
//       But in other environments it works well.
//    void testExecOneliner() {
//        assertEquals "あいうえお" + SEP, TestUtils.executeClientOk(["-e", '"println(\'あいうえお\')"']).text
//    }

    void testExecFile_UTF8() {
        assertEquals "あいうえお" + SEP, TestUtils.executeClientOk(["-c", "UTF-8", "src/test/resources/forEncodingTest_UTF8.groovy"]).text
    }

    void testExecFile_SJIS() {
        assertEquals "あいうえお" + SEP, TestUtils.executeClientOk(["-c", "Shift_JIS", "src/test/resources/forEncodingTest_SJIS.groovy"]).text
    }

}
