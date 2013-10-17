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

import org.jggug.kobo.groovyserv.test.IgnoreForShellClient
import org.jggug.kobo.groovyserv.test.IntegrationTest
import org.jggug.kobo.groovyserv.test.OnlyForShellClient
import org.jggug.kobo.groovyserv.test.TestUtils
import spock.lang.Specification

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
class ClasspathSpec extends Specification {

    def "passing classpath from environment variables"() {
        given:
        def args = ["-e", '"new EnvEcho().echo(\'hello\')"']
        def env = [CLASSPATH: resolvePath('ForClasspathIT_env.jar')]

        when:
        def p = TestUtils.executeClientScriptWithEnv(args, env)
        p.waitFor()

        then:
        p.in.text.contains("Env:hello")
        p.err.text == ""
    }

    def "passing classpath from arguments"() {
        given:
        def args = ["--classpath", resolvePath("ForClasspathIT_arg.jar"), "-e", '"new ArgEcho().echo(\'hello\')"']

        when:
        def p = TestUtils.executeClientScript(args)
        p.waitFor()

        then:
        p.in.text.contains("Arg:hello")
        p.err.text == ""
    }

    def "using argument when classpath is passed from both environment variables and arguments"() {
        given:
        def args = ["--classpath", resolvePath("ForClasspathIT_arg.jar"), "-e", '"new ArgEcho().echo(\'hello\')"']
        def env = [CLASSPATH: resolvePath('ForClasspathIT_env.jar')]

        when:
        def p = TestUtils.executeClientScriptWithEnv(args, env)
        p.waitFor()

        then:
        p.in.text.contains("Arg:hello")
        p.err.text == ""
    }

    @IgnoreForShellClient
    def "propagated classpath is disposed each invocation (except for shell client)"() {
        given:
        def args = ["-e", '"new ArgEcho().echo(\'hello\')"']

        when:
        def p = TestUtils.executeClientScript(args)
        p.waitFor()

        then:
        p.in.text == ""
        p.err.text.contains("org.codehaus.groovy.control.MultipleCompilationErrorsException")
    }

    @OnlyForShellClient
    def "propagated classpath is disposed each invocation (only for shell client)"() {
        given:
        def args = ["-e", '"new ArgEcho().echo(\'hello\')"']

        when:
        def p = TestUtils.executeClientScript(args)
        p.waitFor()

        then:
        p.in.text.contains("org.codehaus.groovy.control.MultipleCompilationErrorsException")
        p.err.text == ""
    }

    private static resolvePath(jarFileName) {
        "${System.properties.'user.dir'}/src/test/resources/${jarFileName}"
    }
}
