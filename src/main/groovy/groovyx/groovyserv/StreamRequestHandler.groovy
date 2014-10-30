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
package groovyx.groovyserv

import groovyx.groovyserv.exception.GServIOException
import groovyx.groovyserv.exception.GServInterruptedException
import groovyx.groovyserv.exception.InvalidRequestHeaderException
import groovyx.groovyserv.utils.LogUtils

/**
 * @author NAKANO Yasuharu
 */
class StreamRequestHandler implements Runnable {

    private ClientConnection conn

    StreamRequestHandler(ClientConnection clientConnection) {
        this.conn = clientConnection
    }

    /**
     * @throws GServInterruptedException
     *             Actually this exception is wrapped by ExecutionException.
     *             When interrupted by client request.
     *             When interrupted by receiving invalid request.
     *             When interrupted by EOF of input stream of socket (Half-closed by the client).
     */
    @Override
    void run() {
        Thread.currentThread().name = "Thread:${StreamRequestHandler.simpleName}"
        LogUtils.debugLog "Thread started"
        try {
            while (true) {
                def request = conn.readStreamRequest()
                if (request.interrupted) {
                    LogUtils.debugLog "Received interruption request from client"
                    throw new GServInterruptedException("By client request")
                }
                if (request.empty) {
                    LogUtils.debugLog "Received empty request from client (Closed stdin on client)"
                    conn.tearDownTransferringPipes()
                    continue // continue to check the client interruption
                }

                def buff = new byte[request.size]
                int offset = 0
                int result = conn.socket.inputStream.read(buff, offset, request.size) // read from raw stream
                if (result == -1) {
                    LogUtils.debugLog "EOF of input stream of socket (Half-closed by the client)"
                    throw new GServInterruptedException("By EOF of input stream of socket")
                }
                readLog(buff, offset, result, request.size)
                if (conn.toreDownPipes) {
                    LogUtils.errorLog "Already tore down pipes. So the above data is just ignored."
                } else {
                    conn.transferStreamRequest(buff, offset, result)
                }
            }
        }
        catch (InvalidRequestHeaderException e) {
            LogUtils.debugLog "Invalid request header: ${e.message}" // ignored details
            throw new GServInterruptedException("By receiving invalid request")
        }
        catch (InterruptedException e) {
            LogUtils.debugLog "Thread interrupted: ${e.message}" // ignored details
        }
        catch (InterruptedIOException e) {
            LogUtils.debugLog "I/O interrupted: ${e.message}" // ignored details
        }
        catch (GServIOException e) {
            LogUtils.debugLog "${e.message}" // ignored details
        }
        catch (IOException e) {
            LogUtils.debugLog "${e.message}" // ignored details
        }
        finally {
            conn.tearDownTransferringPipes()
            LogUtils.debugLog "Thread is dead"
        }
    }

    private static readLog(byte[] buff, int offset, int readSize, int sizeHeader) {
        LogUtils.debugLog """\
            |>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            |Client->Server {
            |  id: in
            |  size(header): ${sizeHeader}
            |  size(actual): ${readSize}
            |  thread group: ${Thread.currentThread().threadGroup.name}
            |  body:
            |${LogUtils.dumpHex(buff, offset, readSize)}
            |}
            |<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            |""".stripMargin()
    }

}

