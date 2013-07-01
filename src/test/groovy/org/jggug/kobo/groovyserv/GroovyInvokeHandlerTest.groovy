/*
 * Copyright 2009-2013 the original author or authors.
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
            clientAuthToken: "DUMMY",
            serverAuthToken: null,
            envVars: ["KEY1=VALUE1"],
            classpath: "CP1${PSEP}CP2${PSEP}CP3",
            args: ['--classpath', "CP6${PSEP}CP7${PSEP}CP8", '-e', 'println("hello")'],
        )
        handler = new GroovyInvokeHandler(request)
    }

    void testRemoveClasspathFromArgs_withEnv_withLongOptClasspath() {
        assert handler.removeClasspathFromArgs(request) == "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        assert request.args == ['-e', 'println("hello")']
    }

    void testRemoveClasspathFromArgs_withEnv_withShortOptCp() {
        request.args = ['-cp', "CP6${PSEP}CP7${PSEP}CP8", '-e', 'println("hello")']
        assert handler.removeClasspathFromArgs(request) == "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        assert request.args == ['-e', 'println("hello")']
    }

    void testRemoveClasspathFromArgs_withEnv_withShortOptClasspath() {
        request.args = ['-classpath', "CP6${PSEP}CP7${PSEP}CP8", '-e', 'println("hello")']
        assert handler.removeClasspathFromArgs(request) == "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        assert request.args == ['-e', 'println("hello")']
    }

    void testRemoveClasspathFromArgs_noEnv_withLongOptClasspath() {
        request.classpath = null
        assert handler.removeClasspathFromArgs(request) == "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        assert request.args == ['-e', 'println("hello")']
    }

    void testRemoveClasspathFromArgs_withEnv_noOptOfClasspath() {
        request.args = ['-e', 'println("hello")']
        assert handler.removeClasspathFromArgs(request) == "CP1${PSEP}CP2${PSEP}CP3${PSEP}."
        assert request.args == ['-e', 'println("hello")']
    }

    void testRemoveClasspathFromArgs_noEnv_noOptOfClasspath() {
        request.classpath = null
        request.args = ['-e', 'println("hello")']
        assert handler.removeClasspathFromArgs(request) == "."
        assert request.args == ['-e', 'println("hello")']
    }

}

