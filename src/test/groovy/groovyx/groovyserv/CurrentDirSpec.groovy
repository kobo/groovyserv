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


import groovyx.groovyserv.test.IntegrationTest
import groovyx.groovyserv.test.TestUtils
import spock.lang.Specification

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
@IntegrationTest
class CurrentDirSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")

    def "executes with specified PWD"() {
        when:
        def result = TestUtils.executeClientCommandWithEnvSuccessfully(["-e", '"println(System.getProperty(\'user.dir\'))"'], [PWD: pwd])

        then:
        result.out == pwd + SEP
        result.err == ""

        where:
        pwd << [sysProp("user.dir"), sysProp("temp.dir"), sysProp("user.home")]
    }

    def "cannot change to different PWD simultaneously while a script is running on another PWD"() {
        given:
        Thread.start {
            TestUtils.executeClientCommand(["-e", '"while(true) { Thread.sleep(500) }"'])
        }

        when:
        def result = TestUtils.executeClientCommandWithEnv(["-e", '"println(System.getProperty(\'user.dir\'))"'], [PWD: File.createTempDir().path])

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: could not change working directory/
            it =~ /Hint:  Another thread may be running on a different working directory\. Wait a moment\./
        }
    }

    private sysProp(String name) {
        System.getProperty(name)
    }
}
