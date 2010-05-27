/*
 * Copyright 2009-2010 the original author or authors.
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

import static java.lang.Thread.currentThread as currentThread
import static org.jggug.kobo.groovyserv.ProtocolHeader.HEADER_SIZE


class MultiplexedInputStream extends InputStream {

    WeakHashMap<ThreadGroup, InputStream> inPerThreadGroup = [:]

    @Override
    public int read() throws IOException {
        int result = currentInputStream.read()
        if (DebugUtils.isVerboseMode() && result != -1) {
            byte[] b = [result]
            DebugUtils.errLog("Client==>Server")
            DebugUtils.errLog(" id=in")
            DebugUtils.errLog(" size=" + result)
            DebugUtils.errLog(DebugUtils.dump(b, 0, 1))
        }
        return result
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = currentInputStream.read(b, off, len)
        if (DebugUtils.isVerboseMode() && result != 0) {
            DebugUtils.errLog("Client==>Server")
            DebugUtils.errLog(" id=in")
            DebugUtils.errLog(" size=" + result)
            DebugUtils.errLog(DebugUtils.dump(b, off, result))
        }
        return result
    }

    @Override
    public int available() throws IOException {
        return currentInputStream.available()
    }

    @Override
    public void close() throws IOException {
        currentInputStream.close()
    }

    @Override
    public void mark(int readlimit) {
        currentInputStream.mark()
    }

    @Override
    public void reset() throws IOException {
        currentInputStream.reset()
    }

    @Override
    public boolean markSupported() {
        currentInputStream.markSupported()
    }

    public void bind(InputStream ins, ThreadGroup tg) {
        def pos = new PipedOutputStream()
        def pis = new PipedInputStream(pos)
        inPerThreadGroup[currentThread().threadGroup] = pis
        // TODO pis which is as System.in for client process each request should be closed when socket is closed.

        // Start a thread for delegating from input stream of socket to System.in
        Thread.startDaemon("inputStreamWorker") {
            try{
                while (true) {
                    //def headers = ProtocolHeader.readHeaders(ins) // FIXME dead lock??
                    def headers = null
                    def sizeHeader = headers[HEADER_SIZE]
                    if (sizeHeader == null) {
                        return
                    }

                    int size = Integer.parseInt(sizeHeader[0])
                    if (size == 0) {
                        return
                    }

                    for (int i = 0; i < size; i++) {
                        int ch = ins.read()
                        if (ch == -1) {
                            break
                        }
                        pos.write(ch)
                    }
                    pos.flush()
                }
            } catch (SocketException e) {
                // Because of here, this daemon thread will be killed when the input stream is closed.
                DebugUtils.verboseLog("input stream is closed.")
            } catch (Throwable t) {
                DebugUtils.verboseLog("unexpected error: throwable=" + t)
            } finally {
                if (pos) pos.close()
            }
        }
    }

    private InputStream getCurrentInputStream() {
        return check(inPerThreadGroup[currentThread().threadGroup])
    }

    private static InputStream check(InputStream ins) {
        if (ins == null) {
            def thread = currentThread()
            throw new IllegalStateException("System.in can't access from this thread: ${thread}:${thread.id}")
        }
        return ins
    }

}
