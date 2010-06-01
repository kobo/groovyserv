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

    @Override
    public int read() {
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
    public int read(byte[] b, int off, int len) {
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
    public int available() {
        currentInputStream.available()
    }

    @Override
    public void close() {
        currentInputStream.close()
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

}
