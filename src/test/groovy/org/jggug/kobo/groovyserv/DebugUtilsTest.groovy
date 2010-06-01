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
 * Tests for the {@link org.jggug.kobo.groovyserv.DebugUtils} class.
 */
class DebugUtilsTest extends GroovyTestCase {

    static final String SEP = System.getProperty("line.separator")

    void testDump_size32() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        assertEquals '\
+-----------+-----------+-----------+-----------+----------------+'+SEP+'\
30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef'+SEP+'\
30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46 | 0123456789ABCDEF'+SEP+'\
+-----------+-----------+-----------+-----------+----------------+'+SEP,
        DebugUtils.dump(data, 0, data.size())
    }

    void testDump_size15() {
        byte[] data = "0123456789abcde".bytes

        assertEquals '\
+-----------+-----------+-----------+-----------+----------------+'+SEP+'\
30 31 32 33 34 35 36 37 38 39 61 62 63 64 65    | 0123456789abcde'+SEP+'\
+-----------+-----------+-----------+-----------+----------------+'+SEP,
        DebugUtils.dump(data, 0, data.size())
    }

    void testDump_size0() {
        byte[] data = "".bytes

        assertEquals '\
+-----------+-----------+-----------+-----------+----------------+'+SEP+'\
+-----------+-----------+-----------+-----------+----------------+'+SEP,
        DebugUtils.dump(data, 0, data.size())
    }

    void testDump_size1_offset1() {
        byte[] data = "0".bytes

        assertEquals '\
+-----------+-----------+-----------+-----------+----------------+'+SEP+'\
+-----------+-----------+-----------+-----------+----------------+'+SEP,
        DebugUtils.dump(data, 1, data.size() - 1)
    }

    void testDump_size32_offset8() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        assertEquals '\
+-----------+-----------+-----------+-----------+----------------+'+SEP+'\
38 39 61 62 63 64 65 66 30 31 32 33 34 35 36 37 | 89abcdef01234567'+SEP+'\
+-----------+-----------+-----------+-----------+----------------+'+SEP,
        DebugUtils.dump(data, 8, 16)
    }


}
