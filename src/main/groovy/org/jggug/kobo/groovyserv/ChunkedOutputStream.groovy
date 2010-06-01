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


class ChunkedOutputStream extends OutputStream {

    String streamId

    private ChunkedOutputStream() { /* preventing from instantiation */ }

    static ChunkedOutputStream newOut() {
        new ChunkedOutputStream(streamId:'o')
    }

    static ChunkedOutputStream newErr() {
        new ChunkedOutputStream(streamId:'e')
    }

    @Override
    public void flush() throws IOException {
        currentOutputStream.flush()
    }

    @Override
    public void close() throws IOException {
        currentOutputStream.close()
    }

    @Override
    public void write(int b) {
        byte[] buf = new byte[1]
        buf[0] = (b>>8*0) & 0x0000ff
        write(buf, 0, 1)
    }

    @Override
    public void write(byte[] b, int offset, int length) {
        if (DebugUtils.isVerboseMode()) {
            DebugUtils.errLog("Server==>Client")
            DebugUtils.errLog(" id=" + streamId)
            DebugUtils.errLog(" size=" + length)
            DebugUtils.errLog(DebugUtils.dump(b, offset, length))
        }
        currentOutputStream.with {
            write(ClientConnection.formatAsResponseHeader(streamId, length))
            write(b, offset, length)
        }
    }

    private OutputStream getCurrentOutputStream() {
        ClientConnectionRepository.instance.currentOut
    }

}

