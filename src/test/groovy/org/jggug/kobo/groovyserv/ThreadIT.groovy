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
class ThreadIT extends GroovyTestCase {

    static final String NL = System.getProperty("line.separator")

    void testExecOneliner() {
        assertEquals(
            'output from thread' + NL,
            TestUtils.execute(["groovyclient", "-e", '"(new Thread({-> println(\'output from thread\') } as Runnable)).start()"']).text
        )
    }

    void testExecFile() {
        assertEquals(
            'output from thread' + NL,
            TestUtils.execute(["groovyclient", "src/test/resources/forThreadTest.groovy"]).text
        )
    }

}
