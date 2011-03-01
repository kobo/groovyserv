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

    void testEnvironmentVariable() {
        def command = TestUtils.getCommand(["-e", '"new EnvEcho().echo(\'hello\')"']) as String[]
        def env = System.env.collect { it.key + "=" + it.value }
        env << "CLASSPATH=${resolvePath('ForClasspathIT_env.jar')}"
        def p = Runtime.runtime.exec(command, env as String[])
        p.waitFor()
        assert p.err.text == ""
        assert p.text.contains("Env:hello")
    }

    void testArguments() {
        def command = TestUtils.getCommand(["--classpath", resolvePath("ForClasspathIT_arg.jar"), "-e", '"new ArgEcho().echo(\'hello\')"']) as String[]
        def p = Runtime.runtime.exec(command)
        p.waitFor()
        assert p.err.text == ""
        assert p.text.contains("Arg:hello")
    }

    void testArguments_withEnvironmentVariables_usingArgIsHigherPriorityThanEnv() {
        def command = TestUtils.getCommand(["--classpath", resolvePath("ForClasspathIT_arg.jar"), "-e", '"new ArgEcho().echo(\'hello\')"']) as String[]
        def env = System.env.collect { it.key + "=" + it.value }
        env << "CLASSPATH=${resolvePath('ForClasspathIT_env.jar')}"
        def p = Runtime.runtime.exec(command, env as String[])
        p.waitFor()
        assert p.err.text == ""
        assert p.text.contains("Arg:hello")
    }

    private resolvePath(jarFileName) {
        "${System.properties.'user.dir'}/src/test/resources/${jarFileName}"
    }

}
