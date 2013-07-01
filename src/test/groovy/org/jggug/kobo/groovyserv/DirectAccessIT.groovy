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

/**
 * Test by using socket directly
 */
class DirectAccessIT extends GroovyTestCase {

    // in response, output of "println" depends on "line.separator" system property
    private static final String SERVER_SIDE_SEPARATOR = System.getProperty("line.separator")

    void testOnlyInvocationRequest() {
        new Socket("localhost", 1961).withStreams { ins, out ->
            out << """\
                |Cwd: /tmp
                |Arg: ${encodeBase64('-e')}
                |Arg: ${encodeBase64('println("hello")')}
                |AuthToken: ${WorkFiles.AUTHTOKEN_FILE.text}
                |
                |""".stripMargin()

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
    }

    void testUsingStreamRequest() {
        use(TestUtils) { // for TestUtils.getAvailableText()
            new Socket("localhost", 1961).withStreams { ins, out ->
                out << """\
                    |Cwd: /tmp
                    |Arg: ${encodeBase64('-e')}
                    |Arg: ${encodeBase64('System.in.eachLine { line, index -> println(line * 2) }')}
                    |AuthToken: ${WorkFiles.AUTHTOKEN_FILE.text}
                    |
                    |""".stripMargin()

                Thread.sleep(500)

                out << String.format("""\
                    |Size: ${SERVER_SIDE_SEPARATOR.size() + 1}
                    |
                    |A%s""".stripMargin(), SERVER_SIDE_SEPARATOR)

                Thread.sleep(500)

                assert ins.availableText == String.format("""\
                    |Channel: out
                    |Size: 2
                    |
                    |AAChannel: out
                    |Size: ${SERVER_SIDE_SEPARATOR.size()}
                    |
                    |%s""".stripMargin(), SERVER_SIDE_SEPARATOR)

                Thread.sleep(500)

                out << String.format("""\
                    |Size: ${SERVER_SIDE_SEPARATOR.size() + 1}
                    |
                    |B%s""".stripMargin(), SERVER_SIDE_SEPARATOR)

                Thread.sleep(500)

                assert ins.availableText == String.format("""\
                    |Channel: out
                    |Size: 2
                    |
                    |BBChannel: out
                    |Size: ${SERVER_SIDE_SEPARATOR.size()}
                    |
                    |%s""".stripMargin(), SERVER_SIDE_SEPARATOR)
            }
        }
    }

    void testInterruptedByClient() {
        use(TestUtils) { // for TestUtils.getAvailableText()
            new Socket("localhost", 1961).withStreams { ins, out ->
                out << """\
                    |Cwd: /tmp
                    |Arg: ${encodeBase64('-e')}
                    |Arg: ${encodeBase64('println "BB"; while (true) { sleep 1000 }')}
                    |AuthToken: ${WorkFiles.AUTHTOKEN_FILE.text}
                    |
                    |""".stripMargin()

                Thread.sleep(1500)

                assert ins.availableText == String.format("""\
                    |Channel: out
                    |Size: 2
                    |
                    |BBChannel: out
                    |Size: ${SERVER_SIDE_SEPARATOR.size()}
                    |
                    |%s""".stripMargin(), SERVER_SIDE_SEPARATOR)

                Thread.sleep(500)

                out << """\
                    |Size: -1
                    |
                    |""".stripMargin()

                Thread.sleep(500)

                assert ins.text == String.format("""\
                    |Status: ${ExitStatus.INTERRUPTED.code}
                    |
                    |""".stripMargin(), SERVER_SIDE_SEPARATOR)

                assert ins.read() == -1
            }
        }
    }

    void testEnvPassing() {
        Random random = new Random(new Date().time)
        String envVarName = "##ENV" + random.nextInt()
        String envVarValue = "##VALUE" + random.nextInt()
        new Socket("localhost", 1961).withStreams { ins, out ->
            out << """\
                |Cwd: /tmp
                |Arg: ${encodeBase64('-e')}
                |Arg: ${encodeBase64("""println System.getenv("$envVarName")""")}
                |Env: $envVarName=$envVarValue
                |AuthToken: ${WorkFiles.AUTHTOKEN_FILE.text}
                |
                |""".stripMargin()

            assert ins.text == String.format("""\
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
    }

    private String encodeBase64(String arg) {
        arg.bytes.encodeBase64()
    }
}

