/*
 * Copyright 2009 the original author or authors.
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
package groovyx.groovyserv

import groovyx.groovyserv.test.UnitTest
import spock.lang.Specification

/**
 * Specifications for the {@link groovyx.groovyserv.GroovyInvokeHandler} class.
 */
@UnitTest
class GroovyInvokeHandlerSpec extends Specification {

    private static final PSEP = File.pathSeparator

    private GroovyInvokeHandler handler
    private InvocationRequest request

    def setup() {
        request = new InvocationRequest(
            port: 1234,
            cwd: "CWD",
            clientAuthToken: "DUMMY",
            serverAuthToken: null,
            envVars: ["KEY1=VALUE1"],
            classpath: null,
            args: null,
        )
        handler = new GroovyInvokeHandler(request)
    }

    def "removeClasspathFromArgs()"() {
        given:
        request.classpath = classpathEnv
        request.args = classpathArg + ['-e', 'println("hello")']

        expect:
        handler.removeClasspathFromArgs(request) == expected
        request.args == ['-e', 'println("hello")']

        where:
        classpathEnv              | classpathArg                               | expected
        "CP1${PSEP}CP2${PSEP}CP3" | ['--classpath', "CP6${PSEP}CP7${PSEP}CP8"] | "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        "CP1${PSEP}CP2${PSEP}CP3" | ['-classpath', "CP6${PSEP}CP7${PSEP}CP8"]  | "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        "CP1${PSEP}CP2${PSEP}CP3" | ['-cp', "CP6${PSEP}CP7${PSEP}CP8"]         | "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        null                      | ['--classpath', "CP6${PSEP}CP7${PSEP}CP8"] | "CP6${PSEP}CP7${PSEP}CP8${PSEP}."
        "CP1${PSEP}CP2${PSEP}CP3" | []                                         | "CP1${PSEP}CP2${PSEP}CP3${PSEP}." // from request
        null                      | []                                         | "."
    }
}

