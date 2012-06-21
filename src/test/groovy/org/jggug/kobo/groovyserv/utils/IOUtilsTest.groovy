/*
 * Copyright 2009-2011 the original author or authors.
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
package org.jggug.kobo.groovyserv.utils

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.utils.IOUtils} class.
 */
class IOUtilsTest extends GroovyTestCase {

    void testReadLines() {
        def ins = new ByteArrayInputStream("AAA\nBBB\nCCC\n\n".bytes)
        def lines = IOUtils.readLines(ins)
        assert lines[0] == "AAA"
        assert lines[1] == "BBB"
        assert lines[2] == "CCC"
        assert lines.size() == 3
    }

    void testReadLine() {
        def ins = new ByteArrayInputStream("AAA\nBBB\nCCC\n\n".bytes)
        assert IOUtils.readLine(ins) == "AAA"
        assert IOUtils.readLine(ins) == "BBB"
        assert IOUtils.readLine(ins) == "CCC"
        assert IOUtils.readLine(ins) == ""
        assert IOUtils.readLine(ins) == ""
        assert IOUtils.readLine(ins) == ""
    }

}
