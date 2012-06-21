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

import org.jggug.kobo.groovyserv.exception.GServInterruptedException
import org.jggug.kobo.groovyserv.exception.GServIOException
import org.jggug.kobo.groovyserv.utils.DebugUtils

/**
 * @author NAKANO Yasuharu
 */
class StreamRequestHandler implements Runnable {

    private String id
    private ClientConnection conn

    StreamRequestHandler(clientConnection) {
        this.id = "StreamRequestHandler:${clientConnection.socket.port}"
        this.conn = clientConnection
    }

    /**
     * @throws GServInterruptedException
     *             Acutally this exception is wrapped by ExecutionException.
     *             When interrupted by client request which has a "Size: -1" header.
     *             When interrupted by receiving invalid request.
     *             When interrupted by EOF of input stream of socket (Half-closed by the client).
     */
    @Override
    void run() {
        Thread.currentThread().name = id
        DebugUtils.verboseLog("${id}: Thread started")
        try {
            while (true) {
                def request = conn.readStreamRequest()
                if (!request.isValid()) {
                    DebugUtils.verboseLog "${id}: 'Size' header is invalid"
                    throw new GServInterruptedException("${id}: By receiving invalid request")
                }
                if (request.isEmpty()) {
                    DebugUtils.verboseLog "${id}: Recieved empty request from client (Closed stdin on client)"
                    conn.tearDownTransferringPipes()
                    continue // continue to check the client interruption
                }
                if (request.isInterrupted()) {
                    DebugUtils.verboseLog "${id}: Recieved interruption request from client"
                    throw new GServInterruptedException("${id}: By client request")
                }

                def buff = new byte[request.size]
                int offset = 0
                int result = conn.socket.inputStream.read(buff, offset, request.size) // read from raw stream
                if (result == -1) {
                    DebugUtils.verboseLog "${id}: EOF of input stream of socket (Half-closed by the client)"
                    throw new GServInterruptedException("${id}: By EOF of input stream of socket")
                }
                readLog(buff, offset, result, request.size)
                if (conn.tearedDownPipes) {
                    DebugUtils.errorLog "Already teared down pipes. So the above data is just ignored."
                } else {
                    conn.transferStreamRequest(buff, offset, result)
                }
            }
        }
        catch (InterruptedException e) {
            DebugUtils.verboseLog("${id}: Thread interrupted: ${e.message}") // ignored details
        }
        catch (InterruptedIOException e) {
            DebugUtils.verboseLog("${id}: I/O interrupted: ${e.message}") // ignored details
        }
        catch (GServIOException e) {
            DebugUtils.verboseLog("${id}: I/O error: ${e.message}") // ignored details
        }
        catch (IOException e) {
            DebugUtils.verboseLog("${id}: I/O error: ${e.message}") // ignored details
        }
        finally {
            conn.tearDownTransferringPipes()
            DebugUtils.verboseLog("${id}: Thread is dead")
        }
    }

    @Override
    String toString() { id }

    private static readLog(byte[] buff, int offset, int readSize, int sizeHeader) {
        DebugUtils.verboseLog """\
            |>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            |Client->Server {
            |  id: in
            |  size(header): ${sizeHeader}
            |  size(actual): ${readSize}
            |  thread group: ${Thread.currentThread().threadGroup.name}
            |  body:
            |${DebugUtils.dump(buff, offset, readSize)}
            |}
            |<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            |""".stripMargin()
    }

}

