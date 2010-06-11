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

import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.CancellationException

import static org.jggug.kobo.groovyserv.ClientConnection.HEADER_SIZE
import static java.lang.Thread.currentThread as currentThread


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
     * @throws GroovyServerIOException
     *             When occured an error on I/O.
     *             Acutally this exception is wrapped by ExecutionException.
     */
    @Override
    void run() {
        Thread.currentThread().name = id
        try {
            while (true) {
                int sizeHeader = getSizeOfHeader()
                if (sizeHeader == null) {
                    DebugUtils.verboseLog "required header 'Size' is not specified."
                    return // not to continue because this is unexpected data
                }
                if (sizeHeader == 0) {
                    DebugUtils.verboseLog "recieved request [Size: 0]"
                    continue
                }
                if (sizeHeader == -1) {
                    DebugUtils.verboseLog "recieved request [Size: -1] to interrupt"
                    throw new ClientInterruptionException("interrupted by client request: [Size: -1]")
                }
                def buff = new byte[sizeHeader]
                int offset = 0
                int result = conn.socket.inputStream.read(buff, offset, sizeHeader) // read from raw stream
                if (result == -1) {
                    // terminate this thread without closing stream.
                    // because to be closed input stream by client doesn't mean termination of session.
                    return
                }
                readLog(buff, offset, result, sizeHeader)
                conn.writeFromStreamRequest(buff, offset, result)
            }
        }
        catch (InterruptedException e) {
            DebugUtils.verboseLog("thread is interrupted: ${id}") // unused exception
        }
        catch (SocketException e) {
            // ignored exception details because passing here is quite general.
            // bevaving as normal return.
            DebugUtils.verboseLog("socket is maybe already closed: ${id}")
        }
        catch (IOException e) {
            throw new GroovyServerIOException("i/o error: ${id}", e)
        }
        finally {
            DebugUtils.verboseLog("thread is done: ${id}")
        }
    }

    private getSizeOfHeader() {
        Map<String, List<String>> headers = conn.readHeaders() // with blocking
        def sizeHeader = headers[HEADER_SIZE]?.getAt(0)
        if (sizeHeader == null) {
            return null
        }
        return sizeHeader as int
    }

    private static readLog(byte[] buff, int offset, int readSize, int sizeHeader) {
        DebugUtils.verboseLog """\
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
Client->Server {
  id: in
  size(header): ${sizeHeader}
  size(actual): ${readSize}
  thread group: ${currentThread().threadGroup.name}
  body:
${DebugUtils.dump(buff, offset, readSize)}
}
<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
"""
    }

}

