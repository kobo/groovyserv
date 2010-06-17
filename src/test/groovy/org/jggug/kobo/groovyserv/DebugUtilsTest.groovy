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

    void setUp() {
        System.properties.remove("groovyserver.verbose")
    }

    void tearDown() {
        System.properties.remove("groovyserver.verbose")
    }

    void testDump_Size32() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef' + SEP +
            '30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46 | 0123456789ABCDEF' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, data.size())
        )
    }

    void testDump_Size31() {
        byte[] data = "0123456789abcdef0123456789ABCDE".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef' + SEP +
            '30 31 32 33 34 35 36 37 38 39 41 42 43 44 45    | 0123456789ABCDE' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, data.size())
        )
    }

    void testDump_Size17() {
        byte[] data = "0123456789abcdef0".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef' + SEP +
            '30                                              | 0' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, data.size())
        )
    }

    void testDump_Size16() {
        byte[] data = "0123456789abcdef".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, data.size())
        )
    }

    void testDump_Size15() {
        byte[] data = "0123456789abcde".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30 31 32 33 34 35 36 37 38 39 61 62 63 64 65    | 0123456789abcde' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, data.size())
        )
    }

    void testDump_Size1() {
        byte[] data = "0".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30                                              | 0' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, data.size())
        )
    }

    void testDump_Size0() {
        byte[] data = "".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, data.size())
        )
    }

    void testDump_Size32_Offset1() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 30 | 123456789abcdef0' + SEP +
            '31 32 33 34 35 36 37 38 39 41 42 43 44 45 46    | 123456789ABCDEF' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 1, data.size())
        )
    }

    void testDump_Size32_Offset31() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '46                                              | F' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 31, data.size())
        )
    }

    void testDump_Size32_Offset32() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 32, data.size())
        )
    }

    void testDump_Size32_Offset33() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 33, data.size())
        )
    }

    void testDump_Size1_Offset1() {
        byte[] data = "0".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 1, data.size())
        )
    }

    void testDump_Size32_Length16() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, 16)
        )
    }

    void testDump_Size32_Length1() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30                                              | 0' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, 1)
        )
    }

    void testDump_Size32_Length0() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, 0)
        )
    }

    void testDump_Size1_Length16() {
        byte[] data = "0".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '30                                              | 0' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 0, 16)
        )
    }

    void testDump_Size32_Offset8_Length16() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '38 39 61 62 63 64 65 66 30 31 32 33 34 35 36 37 | 89abcdef01234567' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 8, 16)
        )
    }

    void testDump_Size32_Offset8_Length15() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        assertEquals(
            '+-----------+-----------+-----------+-----------+----------------+' + SEP +
            '38 39 61 62 63 64 65 66 30 31 32 33 34 35 36    | 89abcdef0123456' + SEP +
            '+-----------+-----------+-----------+-----------+----------------+',
            DebugUtils.dump(data, 8, 15)
        )
    }

    void testDump_NegativeOffset() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        shouldFail(IllegalArgumentException) {
            DebugUtils.dump(data, -1, data.size() - 1)
        }
    }

    void testDump_NegativeLength() {
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes
        shouldFail(IllegalArgumentException) {
            DebugUtils.dump(data, 0, -1)
        }
    }

    void testIsVerboseMode_true() {
        System.properties["groovyserver.verbose"] = "true"
        assert DebugUtils.isVerboseMode()
    }

    void testIsVerboseMode_TRUE() {
        System.properties["groovyserver.verbose"] = "TRUE"
        assert DebugUtils.isVerboseMode()
    }

    void testIsVerboseMode_Null() {
        assert DebugUtils.isVerboseMode() == false
    }

    void testIsVerboseMode_false() {
        System.properties["groovyserver.verbose"] = "false"
        assert DebugUtils.isVerboseMode() == false
    }

}
