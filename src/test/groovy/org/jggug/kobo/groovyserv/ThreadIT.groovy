/*
 * Copyright 2009-2011 the original author or authors.
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

/**
 * Tests for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 */
class ThreadIT extends GroovyTestCase {

    static final String SEP = System.getProperty("line.separator")

    void testExecOneliner() {
        assertEquals(
            'output from thread' + SEP,
            TestUtils.executeClientOk(["-e", '"(new Thread({-> println(\'output from thread\') } as Runnable)).start()"']).text
        )
    }

    void testExecFile() {
        assertEquals(
            'output from thread' + SEP,
            TestUtils.executeClientOk(["-c", "UTF-8", "src/test/resources/forThreadTest.groovy"]).text
        )
    }

    void testInfinteLoopInThread_Interruptable() {
        def script = """\
            Thread.start {
                println('started')
                while (true) {
                    Thread.sleep 100
                }
                println("end")
            }
            Thread.sleep 1000
            Thread.currentThread().interrupt()
        """.replaceAll(/\n/, '; ').replaceAll(/ +/, ' ').trim()
        assertEquals(
            'started' + SEP,
            TestUtils.executeClientOk(["-e", "$script"]).text
        )
    }

    void testInfinteLoopInThread_Uninterruptable() {
        def script = """\
            Thread.start {
                println('started')
                while (true) {
                    /* cannot interruptable */
                }
                println("end")
            }
            Thread.sleep 1000
            Thread.currentThread().interrupt()
        """.replaceAll(/\n/, '; ').replaceAll(/ +/, ' ').trim()
        assertEquals(
            'started' + SEP,
            TestUtils.executeClientOk(["-e", "$script"]).text
        )
    }

}
