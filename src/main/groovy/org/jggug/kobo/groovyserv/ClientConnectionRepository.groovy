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
import static org.jggug.kobo.groovyserv.ClientConnection.HEADER_SIZE


/**
 * A repository of ClientConnection for each ThreadGroup.
 *
 * @author NAKANO Yasuharu
 */
@Singleton
class ClientConnectionRepository {

    private WeakHashMap<ThreadGroup, InputStream> inPerThreadGroup = [:]
    private WeakHashMap<ThreadGroup, OutputStream> outPerThreadGroup = [:]

    void bindIn(InputStream ins, ThreadGroup tg) { // for TOMORROW!!! bind ThreadGroup to not InputStream but ClientConnection. and relationship manager class is needed. I think so.
        def pos = new PipedOutputStream()
        def pis = new PipedInputStream(pos)
        inPerThreadGroup[currentThread().threadGroup] = pis
        // TODO pis which is as System.in for client process each request should be closed when socket is closed.

        // Start a thread for delegating from input stream of socket to System.in
        Thread.startDaemon("inputStreamWorker:${currentThread().threadGroup}") {
            try{
                while (true) {
                    //def headers = connection.readHeaders() // FIXME
                    def headers = null
                    def sizeHeader = headers[HEADER_SIZE]
                    if (sizeHeader == null) {
                        return
                    }

                    int size = Integer.parseInt(sizeHeader[0])
                    if (size == 0) {
                        return
                    }

                    // read body
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
            } catch (Throwable e) {
                DebugUtils.errLog("unexpected error", e)
            } finally {
                if (pos) pos.close()
            }
        }
    }

    void bindOut(OutputStream out, ThreadGroup threadGroup) {
        outPerThreadGroup[threadGroup] = out
    }

    InputStream getCurrentIn() {
        check(inPerThreadGroup[currentThread().threadGroup])
    }

    OutputStream getCurrentOut() {
        check(outPerThreadGroup[currentThread().threadGroup])
    }

    private static check(stream) {
        if (stream == null) {
            def thread = currentThread()
            throw new IllegalStateException("This thread cannot access to standard streams: ${thread}:${thread.id}")
        }
        return stream
    }

}

