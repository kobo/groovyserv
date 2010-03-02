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


import groovy.util.GroovyTestCase

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.Dump} class.
 */
class DumpTest extends GroovyTestCase
{
    static final String NL = System.getProperty("line.separator");

    void testDump32() {
        def baos = new ByteArrayOutputStream()
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        Dump.dump(baos, data, 0, data.size())
        assertEquals '\
+-----------+-----------+-----------+-----------+'+NL+'\
30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef'+NL+'\
30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46 | 0123456789ABCDEF'+NL,
        baos.toString()
    }

    void testDump15() {
        def baos = new ByteArrayOutputStream()
        byte[] data = "0123456789abcde".bytes

        Dump.dump(baos, data, 0, data.size())
        assertEquals '\
+-----------+-----------+-----------+-----------+'+NL+'\
30 31 32 33 34 35 36 37 38 39 61 62 63 64 65    | 0123456789abcde'+NL,
        baos.toString()
    }

    void testDump0() {
        def baos = new ByteArrayOutputStream()
        byte[] data = "".bytes

        baos.reset();
        Dump.dump(baos, data, 0, data.size())

        assertEquals '\
+-----------+-----------+-----------+-----------+'+NL,
        baos.toString()
    }

    void testDump0_ofs1() {
        def baos = new ByteArrayOutputStream()
        byte[] data = "0".bytes

        baos.reset();
        Dump.dump(baos, data, 1, data.size()-1)

        assertEquals '\
+-----------+-----------+-----------+-----------+'+NL,
        baos.toString()
    }

    void testDump32_ofs8() {
        def baos = new ByteArrayOutputStream()
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        Dump.dump(baos, data, 8, 16)

        assertEquals '\
+-----------+-----------+-----------+-----------+'+NL+'\
38 39 61 62 63 64 65 66 30 31 32 33 34 35 36 37 | 89abcdef01234567'+NL,
        baos.toString()
    }


}
