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
 * Handling StreamResponse in protocol between client and server.
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class StreamResponseOutputStream extends OutputStream {

    String streamId

    private StreamResponseOutputStream() { /* preventing from instantiation */ }

    static StreamResponseOutputStream newOut() {
        new StreamResponseOutputStream(streamId:'out')
    }

    static StreamResponseOutputStream newErr() {
        new StreamResponseOutputStream(streamId:'err')
    }

    @Override
    public void flush() {
        currentOutputStream.flush()
    }

    @Override
    public void close() {
        // do nothing here because the OutputStream is connected to socket
    }

    @Override
    public void write(int b) {
        byte[] buf = new byte[1]
        buf[0] = (b>>8*0) & 0x0000ff
        write(buf, 0, 1)
    }

    @Override
    public void write(byte[] b, int offset, int length) {
        writeLog(b, offset, length)
        // FIXME When System.exit to a sub thread which in infinte loop, following synchronized occures IllegalMonitorStateException.
        //synchronized(currentOutputStream) { // to keep independency of 'out' and 'err' on socket stream
            byte[] header = ClientProtocols.formatAsResponseHeader(streamId, length)
            currentOutputStream.write(header)
            currentOutputStream.write(b, offset, length)
        //}
    }

    private writeLog(byte[] b, int offset, int length) {
        DebugUtils.verboseLog """\
            |>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            |Server->Client {
            |  id: ${streamId}
            |  size(actual): ${length}
            |  thread group: ${Thread.currentThread().threadGroup.name}
            |  body:
            |${DebugUtils.dump(b, offset, length)}
            |}
            |<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            |""".stripMargin()
    }

    private OutputStream getCurrentOutputStream() {
        try {
            ClientConnectionRepository.instance.currentConnection.outputStream
        } catch (GServIllegalStateException e) {
            throw new GServIOException("Cannot get a current client connection", e)
        }
    }

}

