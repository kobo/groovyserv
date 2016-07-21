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
import spock.lang.Unroll

@IntegrationTest
class DynamicCurrentDirSpec extends Specification {

    static final String SEP = System.getProperty("line.separator")
    static int port = GroovyServer.DEFAULT_PORT

    def cleanupSpec() {
        restartServer()
    }

    @Unroll
    def "executes with specified PWD (#pwd)"() {
        when:
        def result = TestUtils.executeClientCommandWithWorkDir(["-e", '"println(System.getProperty(\'user.dir\'))"'], new File(pwd))

        then:
        result.assertSuccess()
        result.out == pwd + SEP
        result.err == ""

        where:
        // Not all directories are present on all platforms
        pwd << [sysProp("user.dir"), sysProp("temp.dir"), sysProp("user.home"), sysProp("java.home")].findAll { it != null }
    }

    def "cannot change to different PWD simultaneously while a script is running on another PWD"() {
        given:
        Thread.start {
            TestUtils.executeClientCommand(["-e", '"while(true) { Thread.sleep(500) }"'])
        }
        sleep 500

        when:
        def result = TestUtils.executeClientCommandWithWorkDir(["-e", '"println(System.getProperty(\'user.dir\'))"'], File.createTempDir())

        then:
        result.out == ""
        with(result.err) {
            it =~ /ERROR: could not change working directory/
            it =~ /Hint:  Another thread may be running on a different working directory\. Wait a moment\./
        }
    }

    @Unroll
    def "can run a script under different PWD (#pwd) simultaneously with '-Ckeep-server-cwd' option while a script is running on another PWD"() {
        given:
        def serverCwd = new File(sysProp("user.dir"))
        Thread.start {
            TestUtils.executeClientCommandWithWorkDir(["-e", '"while(true) { Thread.sleep(500) }"'], serverCwd)
        }
        sleep 500

        when:
        def result = TestUtils.executeClientCommandWithWorkDir(["-Ckeep-server-cwd", "-e", '"println(System.getProperty(\'user.dir\'))"'], new File(pwd))

        then:
        result.assertSuccess()
        result.out == serverCwd.absolutePath + SEP // keeping the server CWD
        result.err == ""

        where:
        // Not all directories are present on all platforms
        pwd << [sysProp("temp.dir"), sysProp("user.home"), sysProp("java.home")].findAll { it != null }
    }


    private static restartServer() {
        TestUtils.shutdownServerIfRunning(port)
        TestUtils.startServerIfNotRunning(port)
    }

    private sysProp(String name) {
        System.getProperty(name)
    }
}
