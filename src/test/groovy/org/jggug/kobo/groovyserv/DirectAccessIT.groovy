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
 * Test by using socket directly
 */
class DirectlyAccessIT extends GroovyTestCase {

    void testOnlyInvocationRequest() {
        new Socket("localhost", 1961).withStreams { ins, out ->
// ------------------------
out << """\
Cwd: /tmp
Arg: -e
Arg: println("hello")
Cookie: ${FileUtils.COOKIE_FILE.text}

"""
// ------------------------
assert ins.text == """\
Channel: o
Size: 5

helloChannel: o
Size: 1


Status: 0

""".toString()
        }
    }

    void testUsingStreamRequest() {
        use(TestUtils) { // for TestUtils.getAvailableText()
            new Socket("localhost", 1961).withStreams { ins, out ->
// ------------------------
out << """\
Cwd: /tmp
Arg: -e
Arg: System.in.eachLine { line, index -> println(line * 2) }
Cookie: ${FileUtils.COOKIE_FILE.text}

"""
Thread.sleep(500)
// ------------------------
out << """\
Size: 2

A
"""
Thread.sleep(500)
// ------------------------
assert ins.availableText == """\
Channel: o
Size: 2

AAChannel: o
Size: 1


""".toString()
Thread.sleep(500)
// ------------------------
out << """\
Size: 2

B
"""
Thread.sleep(500)
// ------------------------
assert ins.availableText == """\
Channel: o
Size: 2

BBChannel: o
Size: 1


""".toString()
Thread.sleep(500)
// ------------------------
out << """\
Size: -1

"""
Thread.sleep(500)
// ------------------------
assert ins.availableText == """\
Status: -1

""".toString()
            }
        }
    }

}

