/*
 * Copyright 2009-2011 the original author or authors.
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
    private OutputStream socketOutputStream

    private boolean closed = false
    private boolean tearedDownPipes = false

    // They are used as System.xxx
    final InputStream ins
    final PrintStream out
    final PrintStream err

    ClientConnection(cookie, socket, ownerThreadGroup) {
        this.id = "ClientConnection:${socket.port}"
        this.cookie = cookie
        this.socket = socket
        this.ownerThreadGroup = ownerThreadGroup

        this.pipedOutputStream = new PipedOutputStream()
        this.pipedInputStream = new PipedInputStream(pipedOutputStream)
        this.socketOutputStream = new BufferedOutputStream(socket.outputStream)

        this.ins = StreamRequestInputStream.newIn(pipedInputStream)
        this.out = new PrintStream(StreamResponseOutputStream.newOut(socketOutputStream))
        this.err = new PrintStream(StreamResponseOutputStream.newErr(socketOutputStream))

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
    void transferStreamRequest(byte[] buff, int offset, int result) {
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
     * @throws GServIOException
     */
    void sendExit(int status) {
        try {
            socketOutputStream.with { // not to close yet
                write(ClientProtocols.formatAsExitHeader(status))
                flush()
            }
        } catch (IOException e) {
            throw new GServIOException("${id}: I/O error: failed to send exit status", e)
        }
    }

    /**
     * To close socket and piped I/O stream, and tear down some relational environment.
     * This method closes the actual socket.
     */
    void close() {
        if (closed) {
            DebugUtils.verboseLog "${id}: Already closed"
            return
        }
        tearDownTransferringPipes()
        if (pipedInputStream) {
            IOUtils.close(pipedInputStream)
            DebugUtils.verboseLog "${id}: PipedInputStream is closed"
            pipedInputStream = null
            ins.eof = true
        }
        if (socket) {
            // closing output stream because it needs to flush.
            // socket and socket.inputStream are also closed by closing output stream which is gotten from socket.
            IOUtils.close(socketOutputStream)
            DebugUtils.verboseLog "${id}: Socket is closed"
            socket = null
        }
        ClientConnectionRepository.instance.unbind(ownerThreadGroup)
        closed = true
    }

    /**
     * To close PipedOutputStream as 'stdin'.
     * It's import to stop an user script safety and quietly.
     * When an user script is terminated, you must call this method.
     * Or, IOException with the message of "Write end dead" will be occurred.
     * This method doesn't close the actual socket.
     * The piped input stream isn't closed here. because it's used by a user
     * script after a source of 'stdin' is closed.
     */
    void tearDownTransferringPipes() {
        if (tearedDownPipes) {
            DebugUtils.verboseLog "${id}: Pipes to transfer a stream request teared down"
            return
        }
        if (pipedOutputStream) {
            IOUtils.close(pipedOutputStream)
            DebugUtils.verboseLog "${id}: PipedOutputStream is closed"
            pipedOutputStream = null
        }
        tearedDownPipes = true
    }

    @Override
    String toString() { id }

}

