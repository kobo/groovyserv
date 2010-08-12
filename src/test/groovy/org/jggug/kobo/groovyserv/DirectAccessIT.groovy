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
class DirectAccessIT extends GroovyTestCase {

    // output of "println" depends on "line.separator" system property
    private static final String SEP = System.getProperty("line.separator")

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
Channel: out
Size: 5

helloChannel: out
Size: ${SEP.size()}

${SEP}Status: 0

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
Size: ${SEP.size() + 1}

A${SEP}""".toString()
Thread.sleep(500)
// ------------------------
assert ins.availableText == """\
Channel: out
Size: 2

AAChannel: out
Size: ${SEP.size()}

${SEP}""".toString()
Thread.sleep(500)
// ------------------------
out << """\
Size: ${SEP.size() + 1}

B${SEP}""".toString()
Thread.sleep(500)
// ------------------------
assert ins.availableText == """\
Channel: out
Size: 2

BBChannel: out
Size: ${SEP.size()}

${SEP}""".toString()
Thread.sleep(500)
// ------------------------
out << """\
Size: -1

"""
Thread.sleep(500)
// ------------------------
assert ins.read() == -1
            }
        }
    }

    void testEnvPassig() {
        Random random = new Random(new Date().time)
        String envVarName = "__ENV"+random.nextInt()
        String envVarValue = "__VALUE"+random.nextInt()
        new Socket("localhost", 1961).withStreams { ins, out ->
// ------------------------
out << """\
Cwd: /tmp
Arg: -e
Arg: println System.getenv("$envVarName")
Env: $envVarName=$envVarValue
Cookie: ${FileUtils.COOKIE_FILE.text}

"""
// ------------------------
assert ins.text == """\
Channel: out
Size: ${envVarValue.size()}

${envVarValue}Channel: out
Size: ${SEP.size()}

${SEP}Status: 0

""".toString()

        }
    }

}

