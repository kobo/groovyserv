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
package org.jggug.kobo.groovyserv.platform

import org.jggug.kobo.groovyserv.exception.GServIllegalStateException
import org.jggug.kobo.groovyserv.test.UnitTest
import spock.lang.Specification

/**
 * Specifications for the {@link org.jggug.kobo.groovyserv.platform.CurrentDirHolder} class.
 */
@UnitTest
class CurrentDirHolderSpec extends Specification {

    CurrentDirHolder holder = CurrentDirHolder.instance
    String workDir

    def setup() {
        holder.reset()
        workDir = File.createTempFile("groovyserv-", "-dummy").parent
    }

    def cleanup() {
        holder.reset()
    }

    void "setDir() set currentDir when not set nothing yet"() {
        when:
        holder.setDir(workDir)

        then:
        assertCurrentDir(workDir)
    }

    void "setDir() set currentDir when already set a same directory"() {
        given:
        holder.setDir(workDir)
        assertCurrentDir(workDir)

        when:
        holder.setDir(workDir)

        then:
        assertCurrentDir(workDir)
    }

    def "setDir() set currentDir when already set a different directory"() {
        given:
        holder.setDir(workDir)

        when:
        holder.setDir(workDir + "DIFFERENT")

        then:
        thrown GServIllegalStateException

        and: "previous dir remains"
        assertCurrentDir(workDir)
    }

    def "reset() clears currentDir"() {
        given:
        holder.setDir(workDir)
        assertCurrentDir(workDir)

        when:
        holder.reset()

        then:
        assertReset()
    }

    private void assertCurrentDir(dir) {
        assert holder.currentDir == dir
        assert System.properties['user.dir'] == dir
    }

    private void assertReset() {
        assert holder.currentDir == null
        assert System.properties['user.dir'] == CurrentDirHolder.ORIGINAL_USER_DIR
    }

}

