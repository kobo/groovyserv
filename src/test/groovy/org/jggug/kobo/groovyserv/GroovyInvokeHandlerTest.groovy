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
package org.jggug.kobo.groovyserv

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.GroovyInvokeHandler} class.
 */
class GroovyInvokeHandlerTest extends GroovyTestCase {

    private String originalClasspath
    private GroovyInvokeHandler handler
    private InvocationRequest request
    private PSEP = File.pathSeparator

    void setUp() {
        request = new InvocationRequest(
            port: 1234,
            cwd: "CWD",
            clientCookie: "DUMMY",
            serverCookie: null,
            envVars: ["KEY1=VALUE1"],
            classpath: "CP1${PSEP}CP2${PSEP}CP3",
            args: ['--classpath', "CP6${PSEP}CP7${PSEP}CP8", '-e', 'println("hello")'],
        )
        handler = new GroovyInvokeHandler(request)
    }

    void testCompleteClasspathArg_withEnv_withLongOptClasspath() {
        handler.completeClasspathArg(request)
        assert request.args == ['--classpath', "\"CP6${PSEP}CP7${PSEP}CP8${PSEP}.\"", '-e', 'println("hello")']
    }

    void testCompleteClasspathArg_withEnv_withShortOptCp() {
        request.args = ['-cp', "CP6${PSEP}CP7${PSEP}CP8", '-e', 'println("hello")']
        handler.completeClasspathArg(request)
        assert request.args == ['--classpath', "\"CP6${PSEP}CP7${PSEP}CP8${PSEP}.\"", '-e', 'println("hello")']
    }

    void testCompleteClasspathArg_withEnv_withShortOptClasspath() {
        request.args = ['-classpath', "CP6${PSEP}CP7${PSEP}CP8", '-e', 'println("hello")']
        handler.completeClasspathArg(request)
        assert request.args == ['--classpath', "\"CP6${PSEP}CP7${PSEP}CP8${PSEP}.\"", '-e', 'println("hello")']
    }

    void testCompleteClasspathArg_noEnv_withLongOptClasspath() {
        request.classpath = null
        handler.completeClasspathArg(request)
        assert request.args == ['--classpath', "\"CP6${PSEP}CP7${PSEP}CP8${PSEP}.\"", '-e', 'println("hello")']
    }

    void testCompleteClasspathArg_withEnv_noOptOfClasspath() {
        request.args = ['-e', 'println("hello")']
        handler.completeClasspathArg(request)
        assert request.args == ['--classpath', "\"CP1${PSEP}CP2${PSEP}CP3${PSEP}.\"", '-e', 'println("hello")']
    }

    void testCompleteClasspathArg_noEnv_noOptOfClasspath() {
        request.classpath = null
        request.args = ['-e', 'println("hello")']
        handler.completeClasspathArg(request)
        assert request.args == ['--classpath', "\".\"", '-e', 'println("hello")']
    }

}

