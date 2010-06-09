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
 * Handling StreamRequest in protocol between client and server.
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class StreamRequestInputStream extends InputStream {

    @Override
    public int read() {
        return currentInputStream.read()
    }

    @Override
    public int read(byte[] buf, int offset, int length) {
        return currentInputStream.read(buf, offset, length)
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
        ClientConnectionRepository.instance.currentConnection.inputStream
    }

}
