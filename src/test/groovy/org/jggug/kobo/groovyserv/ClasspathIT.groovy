/*
 * Copyright 2009-2010 the original author or authors.
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

import groovy.util.GroovyTestCase

/**
 * Tests for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
class ClasspathIT extends GroovyTestCase {

    static final String EOP = System.getProperty("path.separator")

    void testAddedClasspathFromCilent_single() {
        def command = TestUtils.getCommand(["-e", '"println(System.properties.\'groovy.classpath\')"']) as String[]
        def env = System.env.collect { it.key + "=" + it.value }
        env << "CLASSPATH=HOGE"
        def p = Runtime.runtime.exec(command, env as String[])
        p.waitFor()
        assert p.err.text == ""
        def result = p.text
        assert result.contains(System.properties.'user.dir')
        assert result.contains("HOGE")
    }

    void testAddedClasspathFromCilent_multiple() {
        def command = TestUtils.getCommand(["-e", '"println(System.properties.\'groovy.classpath\')"']) as String[]
        def env = System.env.collect { it.key + "=" + it.value }
        env << "CLASSPATH=HOGE${EOP}FOO${EOP}BAR"
        def p = Runtime.runtime.exec(command, env as String[])
        p.waitFor()
        assert p.err.text == ""
        def result = p.text
        assert result.contains(System.properties.'user.dir')
        assert result.contains("HOGE")
        assert result.contains("FOO")
        assert result.contains("BAR")
    }

}
