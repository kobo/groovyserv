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


/**
 * @author NAKANO Yasuharu
 */
class StreamRequestHandler implements Runnable {

    private String id
    private ClientConnection conn

    StreamRequestHandler(clientConnection) {
        this.id = "GroovyServ:StreamRequestHandler:${clientConnection.socket.port}"
        this.conn = clientConnection
    }

    /**
     * @throws ClientInterruptionException
     *             When interrupted by client request which has a "Size: -1" header.
     *             Acutally this exception is wrapped by ExecutionException.
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
                    return // not to continue because this is unexpected data
                }
                if (request.isEmpty()) {
                    DebugUtils.verboseLog "${id}: Recieved empty request from client"
                    throw new ClientInterruptionException("${id}: Empty request by client request")
                }
                if (request.isInterrupted()) {
                    DebugUtils.verboseLog "${id}: Recieved interrupted request from client"
                    throw new ClientInterruptionException("${id}: Interrupted by client request")
                }
                def buff = new byte[request.size]
                int offset = 0
                int result = conn.socket.inputStream.read(buff, offset, request.size) // read from raw stream
                if (result == -1) {
                    // terminate this thread without closing stream.
                    // because to be closed input stream by client doesn't mean termination of session.
                    DebugUtils.verboseLog "${id}: End of stream"
                    return
                }
                readLog(buff, offset, result, request.size)
                conn.writeFromStreamRequest(buff, offset, result)
            }
        }
        catch (InterruptedException e) {
            DebugUtils.verboseLog("${id}: Thread interrupted: ${e.message}") // ignored details
        }
        catch (GServIOException e) {
            DebugUtils.verboseLog("${id}: I/O error: ${e.message}") // ignored details
        }
        catch (InterruptedIOException e) {
            DebugUtils.verboseLog("${id}: I/O interrupted: ${e.message}") // ignored details
        }
        catch (IOException e) {
            DebugUtils.verboseLog("${id}: I/O error: ${e.message}") // ignored details
        }
        finally {
            DebugUtils.verboseLog("${id}: Thread is dead")
        }
    }

    @Override
    String toString() { id }

    private static readLog(byte[] buff, int offset, int readSize, int sizeHeader) {
        DebugUtils.verboseLog """\
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
Client->Server {
  id: in
  size(header): ${sizeHeader}
  size(actual): ${readSize}
  thread group: ${Thread.currentThread().threadGroup.name}
  body:
${DebugUtils.dump(buff, offset, readSize)}
}
<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
"""
    }

}

