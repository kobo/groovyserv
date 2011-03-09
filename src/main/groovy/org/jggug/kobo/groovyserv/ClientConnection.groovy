/*
 * Copyright 2009-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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


/**
 * @author NAKANO Yasuharu
 */
class ClientConnection implements Closeable {

    private String id
    private Cookie cookie
    private Socket socket
    private ThreadGroup ownerThreadGroup
    private PipedOutputStream pipedOutputStream // to transfer from socket.inputStream
    private PipedInputStream pipedInputStream   // connected to socket.inputStream indirectly via pipedInputStream and used as System.in

    ClientConnection(cookie, socket, ownerThreadGroup) {
        this.id = "ClientConnection:${socket.port}"
        this.cookie = cookie
        this.socket = socket
        this.ownerThreadGroup = ownerThreadGroup
        this.pipedOutputStream = new PipedOutputStream()
        this.pipedInputStream = new PipedInputStream(pipedOutputStream)
        ClientConnectionRepository.instance.bind(ownerThreadGroup, this)
    }

    /**
     * @throws InvalidRequestHeaderException
     * @throws GServIOException
     */
    InvocationRequest openSession() {
        ClientProtocols.readInvocationRequest(this)
    }

    /**
     * @throws InvalidRequestHeaderException
     * @throws GServIOException
     */
    StreamRequest readStreamRequest() {
        ClientProtocols.readStreamRequest(this)
    }

    /**
     * @throws GServIOException
     */
    void writeFromStreamRequest(byte[] buff, int offset, int result) {
        try {
            pipedOutputStream.write(buff, offset, result)
            pipedOutputStream.flush()
        } catch (InterruptedIOException e) {
            throw new GServIOException("${id}: I/O interrupted: Failed to write to piped stream", e)
        } catch (IOException e) {
            throw new GServIOException("${id}: I/O error: Failed to write to piped stream", e)
        }
    }

    /**
     * to return InputStream which you can use as System.in.
     */
    InputStream getInputStream() {
        pipedInputStream
    }

    /**
     * to return InputStream which you can use as System.out or System.err.
     */
    OutputStream getOutputStream() {
        socket.outputStream
    }

    /**
     * @throws GServIOException
     */
    void sendExit(int status) {
        try {
            socket.outputStream.with { // not to close yet
                write(ClientProtocols.formatAsExitHeader(status))
                flush()
            }
        } catch (IOException e) {
            throw new GServIOException("${id}: I/O error: failed to send exit status", e)
        }
    }

    /**
     * To close socket and tear down some relational environment.
     */
    void close() {
        ClientConnectionRepository.instance.unbind(ownerThreadGroup)
        if (pipedInputStream) {
            IOUtils.close(pipedInputStream)
            DebugUtils.verboseLog "${id}: Piped stream closed"
            pipedInputStream = null
        }
        if (socket) {
            // closing output stream because it needs to flush.
            // socket and socket.inputStream are also closed by closing output stream which is gotten from socket.
            IOUtils.close(socket.outputStream)
            DebugUtils.verboseLog "${id}: Socket closed"
            socket = null
        }
    }

    @Override
    String toString() { id }

}

