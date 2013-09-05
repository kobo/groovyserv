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
package org.jggug.kobo.groovyserv

import org.jggug.kobo.groovyserv.utils.IOUtils

/**
 * GroovyServ's client implemented by Groovy.
 * <p>
 * Mainly for testing.
 *
 * @author Yasuharu NAKANO
 */
class GroovyClient {

    // in response, output of "println" depends on "line.separator" system property
    private static final String SERVER_SIDE_SEPARATOR = System.getProperty("line.separator")

    private String host
    private int port

    private Socket socket
    private InputStream ins
    private OutputStream out

    private List<String> env = []

    private outBytes = []
    private errBytes = []
    private Integer exitStatus

    def GroovyClient(String host = "localhost", int port = 1961) {
        this.host = host
        this.port = port
    }

    void run(String... args) {
        connect()

        def headers = [
            Cwd: "/tmp",
            Auth: WorkFiles.AUTHTOKEN_FILE.text,
        ]
        if (args) headers.Arg = args.collect { it.bytes.encodeBase64() }
        if (env) headers.Env = env
        out << ClientProtocols.formatAsHeader(headers)
    }

    void input(String text) {
        checkActive()

        text += SERVER_SIDE_SEPARATOR
        out << """\
                |Size: ${text.size()}
                |
                |${text}""".stripMargin()
    }

    void readAll() {
        checkActive()
        clearBuffer()

        def availableText = IOUtils.readAvailableText(ins)
        if (availableText.empty) return

        def availableInputStream = new ByteArrayInputStream(availableText.bytes)
        while (true) {
            def headers = ClientProtocols.parseHeaders("GroovyClient:${socket.port}", availableInputStream)
            if (!headers) break

            if (headers.Channel?.getAt(0) ==~ /out|err/) {
                def buff = this."${headers.Channel?.get(0)}Bytes"
                int size = headers.Size?.getAt(0) as int
                buff.addAll ClientProtocols.readBody(availableInputStream, size)
            } else if (headers.Status) {
                exitStatus = headers.Status?.getAt(0) as int
            }
        }

        if (exitStatus) disconnect()
    }

    String getOutText() {
        return new String(outBytes.flatten() as byte[])
    }

    String getErrText() {
        return new String(errBytes.flatten() as byte[])
    }

    int getExitStatus() {
        exitStatus
    }

    void interrupt() {
        checkActive()

        out << ClientProtocols.formatAsHeader([
            Auth: WorkFiles.AUTHTOKEN_FILE.text,
            Cmd: "interrupt",
        ])
    }

    void shutdown() {
        checkActive()

        out << ClientProtocols.formatAsHeader([
            Auth: WorkFiles.AUTHTOKEN_FILE.text,
            Cmd: "shutdown",
        ])
    }

    private void clearBuffer() {
        outBytes.clear()
        errBytes.clear()
        exitStatus = null
    }

    private connect() {
        socket = new Socket(host, port)
        ins = socket.inputStream
        out = socket.outputStream
    }

    private disconnect() {
        socket.close()
        socket = null
        ins = null
        out = null
    }

    private void checkActive() {
        if (socket == null) throw new IllegalStateException("Not started yet")
    }
}