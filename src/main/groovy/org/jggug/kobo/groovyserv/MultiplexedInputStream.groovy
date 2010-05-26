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

class MultiplexedInputStream extends InputStream {

    static WeakHashMap<ThreadGroup, InputStream>map = [:]

    @Override
    public int read() throws IOException {
        int result = getCurrentInputStream().read()
        if (result != -1 && System.getProperty("groovyserver.verbose") == "true") {
            byte[] b = [result]
            if (System.getProperty("groovyserver.verbose") == "true") {
                StreamManager.errLog("Client==>Server")
            }
            Dump.dump(originalStreams.err, b, 0, 1)
        }
        return result
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = getCurrentInputStream().read(b, off, len)
        if (result != 0 && System.getProperty("groovyserver.verbose") == "true") {
            StreamManager.errLog("Client==>Server")
            StreamManager.errLog(" id=in")
            StreamManager.errLog(" size="+result)
            Dump.dump(originalStreams.err, b, off, result)
        }
        return result
    }

    @Override
    public int available() throws IOException {
        return getCurrentInputStream().available()
    }

    @Override
    public void close() throws IOException {
        getCurrentInputStream().close()
    }

    @Override
    public void mark(int readlimit) {
        getCurrentInputStream().mark()
    }

    @Override
    public void reset() throws IOException {
        getCurrentInputStream().reset()
    }

    @Override
    public boolean markSupported() {
        getCurrentInputStream().markSupported()
    }

    public void bind(InputStream ins, ThreadGroup tg) {
        def pos = new PipedOutputStream()
        def pis = new PipedInputStream(pos)
        Thread streamCopyWorker = new Thread({
            try{
                while (true) {
                    def headers = GroovyServer.readHeaders(ins)
                    def sizeHeader = headers[ChunkedOutputStream.HEADER_SIZE]
                    if (sizeHeader != null) {
                        def size = Integer.parseInt(sizeHeader[0])
                        if (size == 0) {
                            pos.close()
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
                    else {
                        pos.close()
                        return
                    }
                }
            }
            catch (java.net.SocketException e) {
                if (System.getProperty("groovyserver.verbose") == "true") {
                    StreamManager.errLog("ins closed.")
                }
            }
            catch (Throwable t) {
                if (System.getProperty("groovyserver.verbose") == "true") {
                    StreamManager.errLog("t="+t)
                }
            }
        } as Runnable, "streamWorker")
        map[Thread.currentThread().getThreadGroup()] = pis
        streamCopyWorker.setDaemon(true)
        streamCopyWorker.start()
    }

    private InputStream getCurrentInputStream() {
        return check(map[Thread.currentThread().getThreadGroup()])
    }

    private InputStream check(InputStream ins) {
        if (ins == null) {
            throw new IllegalStateException("System.in can't access from this thread: "+Thread.currentThread()+":"+Thread.currentThread().id)
        }
        return ins
    }

}
