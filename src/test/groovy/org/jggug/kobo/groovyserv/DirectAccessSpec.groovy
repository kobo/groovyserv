/*
 * Copyright 2009-2013 the original author or authors.
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

import org.jggug.kobo.groovyserv.test.IntegrationTest
import org.jggug.kobo.groovyserv.test.TestUtils
import spock.lang.Specification

/**
 * Specifications when using socket directly
 */
@IntegrationTest
class DirectAccessSpec extends Specification {

    // in response, output of "println" depends on "line.separator" system property
    private static final String SERVER_SIDE_SEPARATOR = System.getProperty("line.separator")

    Socket socket
    InputStream ins
    OutputStream out

    def setup() {
        socket = new Socket("localhost", 1961)
        ins = socket.inputStream
        out = socket.outputStream
    }

    def cleanup() {
        socket.close()
    }

    def "only InvocationRequest"() {
        when:
        out << """\
            |Cwd: /tmp
            |Arg: ${encodeBase64('-e')}
            |Arg: ${encodeBase64('println("hello")')}
            |Auth: ${WorkFiles.AUTHTOKEN_FILE.text}
            |
            |""".stripMargin()

        then:
        // The reason of using String.format is that stripMargin forcely converts all line separators to "\n".
        assert ins.text == String.format("""\
            |Channel: out
            |Size: 5
            |
            |helloChannel: out
            |Size: ${SERVER_SIDE_SEPARATOR.size()}
            |
            |%sStatus: 0
            |
            |""".stripMargin(), SERVER_SIDE_SEPARATOR)
    }

    def "using StreamRequest"() {
        when:
        out << """\
            |Cwd: /tmp
            |Arg: ${encodeBase64('-e')}
            |Arg: ${encodeBase64('System.in.eachLine { line, index -> println(line * 2) }')}
            |Auth: ${WorkFiles.AUTHTOKEN_FILE.text}
            |
            |""".stripMargin()
        Thread.sleep(500)

        and:
        out << String.format("""\
            |Size: ${SERVER_SIDE_SEPARATOR.size() + 1}
            |
            |A%s""".stripMargin(), SERVER_SIDE_SEPARATOR)
        Thread.sleep(500)

        then:
        retrieveAvailableText(ins) == String.format("""\
            |Channel: out
            |Size: 2
            |
            |AAChannel: out
            |Size: ${SERVER_SIDE_SEPARATOR.size()}
            |
            |%s""".stripMargin(), SERVER_SIDE_SEPARATOR)

        when:
        Thread.sleep(500)
        out << String.format("""\
            |Size: ${SERVER_SIDE_SEPARATOR.size() + 1}
            |
            |B%s""".stripMargin(), SERVER_SIDE_SEPARATOR)
        Thread.sleep(500)

        then:
        retrieveAvailableText(ins) == String.format("""\
            |Channel: out
            |Size: 2
            |
            |BBChannel: out
            |Size: ${SERVER_SIDE_SEPARATOR.size()}
            |
            |%s""".stripMargin(), SERVER_SIDE_SEPARATOR)
    }

    def "interrupted by client"() {
        when:
        out << """\
            |Cwd: /tmp
            |Arg: ${encodeBase64('-e')}
            |Arg: ${encodeBase64('println "BB"; while (true) { sleep 1000 }')}
            |Auth: ${WorkFiles.AUTHTOKEN_FILE.text}
            |
            |""".stripMargin()
        Thread.sleep(1500)

        then:
        retrieveAvailableText(ins) == String.format("""\
            |Channel: out
            |Size: 2
            |
            |BBChannel: out
            |Size: ${SERVER_SIDE_SEPARATOR.size()}
            |
            |%s""".stripMargin(), SERVER_SIDE_SEPARATOR)

        when:
        Thread.sleep(500)
        out << """\
            |Cmd: interrupt
            |
            |""".stripMargin()
        Thread.sleep(500)

        then:
        ins.text == String.format("""\
            |Status: ${ExitStatus.INTERRUPTED.code}
            |
            |""".stripMargin(), SERVER_SIDE_SEPARATOR)

        and:
        ins.read() == -1
    }

    def "passing environment variables"() {
        given:
        Random random = new Random(new Date().time)
        String envVarName = "##ENV" + random.nextInt()
        String envVarValue = "##VALUE" + random.nextInt()

        when:
        out << """\
            |Cwd: /tmp
            |Arg: ${encodeBase64('-e')}
            |Arg: ${encodeBase64("""println System.getenv("$envVarName")""")}
            |Env: $envVarName=$envVarValue
            |Auth: ${WorkFiles.AUTHTOKEN_FILE.text}
            |
            |""".stripMargin()

        then:
        ins.text == String.format("""\
            |Channel: out
            |Size: ${envVarValue.size()}
            |
            |${envVarValue}Channel: out
            |Size: ${SERVER_SIDE_SEPARATOR.size()}
            |
            |%sStatus: 0
            |
            |""".stripMargin(), SERVER_SIDE_SEPARATOR)
    }

    private static String encodeBase64(String arg) {
        arg.bytes.encodeBase64()
    }

    private static String retrieveAvailableText(ins) {
        use(TestUtils) {
            return ins.availableText
        }
    }
}
