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

import org.jggug.kobo.groovyserv.exception.InvalidAuthTokenException
import org.jggug.kobo.groovyserv.utils.IOUtils
import org.jggug.kobo.groovyserv.utils.LogUtils

/**
 * GroovyServ's client implemented by Groovy.
 *
 * @author Yasuharu NAKANO
 */
class GroovyClient {

    // in response, output of "println" depends on "line.separator" system property
    private static final String SERVER_SIDE_SEPARATOR = System.getProperty("line.separator")

    private String host
    private int port

    private String authtoken
    private Socket socket
    private InputStream ins
    private OutputStream out

    List<String> environments = []

    private outBytes = []
    private errBytes = []
    private Integer exitStatus

    GroovyClient(String host = "localhost", int port = GroovyServer.DEFAULT_PORT) {
        this.host = host
        this.port = port
    }

    GroovyClient run(String... args) {
        checkActive()
        def headers = [
            Cwd: "/tmp",
            Auth: authtoken,
        ]
        if (args) headers.Arg = args.collect { it.bytes.encodeBase64() }
        if (environments) headers.Env = environments
        out << ClientProtocols.formatAsHeader(headers)
        return this // for method-chain
    }

    GroovyClient input(String text) {
        checkActive()
        text += SERVER_SIDE_SEPARATOR
        out << """\
                |Size: ${text.size()}
                |
                |${text}""".stripMargin()
        return this // for method-chain
    }

    void readAllAvailable() {
        checkActive()
        clearBuffer()

        def availableText = IOUtils.readAvailableText(ins)
        if (availableText.empty) return

        def availableInputStream = new ByteArrayInputStream(availableText.bytes)
        while (true) {
            def headers = ClientProtocols.parseHeaders(availableInputStream)
            if (!headers) break

            if (headers.Channel?.getAt(0)==~/out|err/) {
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

    GroovyClient interrupt() {
        checkActive()
        out << ClientProtocols.formatAsHeader(
            Auth: authtoken,
            Cmd: "interrupt",
        )
        return this // for method-chain
    }

    GroovyClient shutdown() {
        checkActive()
        out << ClientProtocols.formatAsHeader(
            Auth: authtoken,
            Cmd: "shutdown",
        )
        return this // for method-chain
    }

    GroovyClient ping() {
        checkActive()
        out << ClientProtocols.formatAsHeader(
            Auth: authtoken,
            Cmd: "ping",
        )
        return this // for method-chain
    }

    GroovyClient waitFor() {
        while (exitStatus == null) {
            sleep 100 // wait for server operation
            readAllAvailable()
        }
        return this // for method-chain
    }

    boolean isServerAvailable() {
        if (!WorkFiles.AUTHTOKEN_FILE.exists()) {
            LogUtils.debugLog "No authtoken file"
            return false
        }
        try {
            if (!isConnected()) connect()
            ping()
            waitFor()
        }
        catch (ConnectException e) {
            LogUtils.debugLog "Caught exception when health-checking", e
            return false
        }
        catch (Exception e) {
            LogUtils.errorLog "Caught unexpected exception when health-checking", e
            return false
        }
        finally {
            disconnect()
        }

        if (exitStatus == ExitStatus.INVALID_AUTHTOKEN.code) {
            throw new InvalidAuthTokenException("Authentication failed")
        }
        if (exitStatus != ExitStatus.SUCCESS.code) {
            LogUtils.errorLog "Exit status for ping seems invalid: $exitStatus"
        }
        return true
    }

    // NOTE: isServerAvailable() isn't !isServerShutdown()
    // Because a complete shutdown status must be port closed and authtoken file deleted.
    // On the other hand, server available status means a server can handle a request rightly.
    // The contrary means a server cannot handle a request just rightly.
    // Either that a port is closed or that a authtoken file is deleted happen to match the condition.
    boolean isServerShutdown() {
        if (WorkFiles.AUTHTOKEN_FILE.exists()) {
            return false
        }
        return !canConnect()
    }

    boolean canConnect() {
        try {
            new Socket(host, port).close()
        }
        catch (ConnectException e) {
            return false
        }
        catch (Exception e) {
            LogUtils.errorLog "Caught unexpected exception when health-checking", e
            return false
        }
        return true
    }

    private void clearBuffer() {
        outBytes.clear()
        errBytes.clear()
        exitStatus = null
    }

    GroovyClient connect() {
        checkInactive()
        if (!WorkFiles.AUTHTOKEN_FILE.exists()) {
            throw new IllegalStateException("No authtoken file")
        }
        authtoken = WorkFiles.AUTHTOKEN_FILE.text
        socket = new Socket(host, port)
        ins = socket.inputStream
        out = socket.outputStream

        return this // for method-chain
    }

    GroovyClient disconnect() {
        if (connected) {
            socket.close()
        }
        socket = null
        ins = null
        out = null

        return this // for method-chain
    }

    @Override
    protected void finalize() throws Throwable {
        disconnect()
        super.finalize()
    }

    private void checkActive() {
        if (!connected) throw new IllegalStateException("Connection is disabled")
    }

    private void checkInactive() {
        if (connected) throw new IllegalStateException("Already connected to server")
    }

    private boolean isConnected() {
        socket != null && !socket.closed
    }
}
