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

import static org.jggug.kobo.groovyserv.ClientConnection.HEADER_SIZE
import static java.lang.Thread.currentThread as currentThread


/**
 * Handling StreamRequest in protocol between client and server.
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class StreamRequestInputStream extends InputStream {

    @Override
    public int read() {
        int sizeHeader = getSizeOfHeader()
        if (sizeHeader == -1) {
            DebugUtils.verboseLog "recieved request [Size: -1] to interrupt"
            throw new ExitException(-1, "interrupted by client request: [Size: -1]")
        }
        if (sizeHeader == 0) {
            DebugUtils.verboseLog "recieved request [Size: 0]"
            return 0
        }
        int result = currentInputStream.read()
        if (result != -1) {
            readLog([result], 0, 1, sizeHeader)
        }
        return result
    }

    @Override
    public int read(byte[] buf, int offset, int length) {
        int sizeHeader = getSizeOfHeader()
        if (sizeHeader == -1) {
            DebugUtils.verboseLog "recieved request [Size: -1] to interrupt"
            throw new ExitException(-1, "interrupted by client request: [Size: -1]")
        }
        if (sizeHeader == 0) {
            DebugUtils.verboseLog "recieved request [Size: 0]"
            return 0
        }
        int revisedLength = Math.min(length, sizeHeader)
        int result = currentInputStream.read(buf, offset, revisedLength)
        if (result != -1) {
            readLog(buf, offset, result, sizeHeader, revisedLength)
        }
        return result
    }

    private int getSizeOfHeader() {
        Map<String, List<String>> headers = currentConnection.readHeaders()
        def sizeHeader = headers[HEADER_SIZE]?.getAt(0)
        if (sizeHeader == null) {
            throw new GroovyServerException("required header 'Size' is not specified.")
        }
        return sizeHeader as int
    }

    private static readLog(byte[] buff, int offset, int readSize, int sizeHeader, int revisedLength) {
        DebugUtils.verboseLog """\
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
Client->Server {
  id: in
  size(header): ${sizeHeader}
  size(revised): ${revisedLength}
  size(actual): ${readSize}
  thread group: ${currentThread().threadGroup.name}
  body:
${DebugUtils.dump(buff, offset, readSize)}
}
<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
"""
    }

    @Override
    public void close() {
        // do nothing here because the InputStream is connected to socket
    }

    @Override
    public void mark(int readlimit) {
        currentInputStream.mark()
    }

    @Override
    public void reset() {
        currentInputStream.reset()
    }

    @Override
    public boolean markSupported() {
        currentInputStream.markSupported()
    }

    private InputStream getCurrentInputStream() {
        ClientConnectionRepository.instance.currentIn
    }

    private ClientConnection getCurrentConnection() {
        ClientConnectionRepository.instance.currentConnection
    }

}
