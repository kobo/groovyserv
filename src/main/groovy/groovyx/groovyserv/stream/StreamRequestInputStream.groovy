/*
 * Copyright 2009 the original author or authors.
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
package groovyx.groovyserv.stream

import groovyx.groovyserv.ClientConnection
import groovyx.groovyserv.utils.LogUtils

/**
 * Handling StreamRequest in protocol between client and server.
 *
 * @author NAKANO Yasuharu
 */
class StreamRequestInputStream extends InputStream {

    private InputStream inputStream
    private boolean closed = false

    private StreamRequestInputStream() { /* preventing from instantiation */ }

    static StreamRequestInputStream newIn(InputStream inputStream) {
        new StreamRequestInputStream(inputStream: inputStream)
    }

    /**
     * @throws IOException When the stream is already closed
     */
    @Override
    int read() {
        if (closed) throw new IOException("Stream of channel 'in' already closed")
        try {
            return inputStream.read()
        } catch (InterruptedIOException e) {
            LogUtils.debugLog "StreamRequestInputStream:read(): Interrupted I/O"
            return -1
        }
    }

    /**
     * @throws IOException When the stream is already closed
     */
    @Override
    int read(byte[] buf, int offset, int length) {
        if (closed) throw new IOException("Stream of channel 'in' already closed")
        try {
            return inputStream.read(buf, offset, length)
        } catch (InterruptedIOException e) {
            LogUtils.debugLog "StreamRequestInputStream:read(byte[], int, int): Interrupted I/O"
            return -1
        }
    }

    @Override
    void close() {
        closed = true
        LogUtils.debugLog "StreamRequestInputStream is closed: ${ClientConnection.currentConnection}"
    }

    /**
     * @throws IOException When the stream is already closed
     */
    @Override
    void mark(int readlimit) {
        if (closed) throw new IOException("Stream of channel 'in' already closed")
        inputStream.mark()
    }

    /**
     * @throws IOException When the stream is already closed
     */
    @Override
    void reset() {
        if (closed) throw new IOException("Stream of channel 'in' already closed")
        inputStream.reset()
    }

    /**
     * @throws IOException When the stream is already closed
     */
    @Override
    boolean markSupported() {
        if (closed) throw new IOException("Stream of channel 'in' already closed")
        inputStream.markSupported()
    }

}
