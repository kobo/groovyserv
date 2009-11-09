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
package org.jggug.kobo.groovyserv

class ChunkedPrintStream extends PrintStream {

    final String HEADER_STREAM_ID = "Id";
    final String HEADER_CHUNK_SIZE = "Size";

    char streamIdentifier;

    @Override
    public void write(int b) {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {

        if (System.getProperty("groovyserver.verbose") == "true") {
            GroovyServer.originalErr.println("id="+streamIdentifier);
            GroovyServer.originalErr.println("size="+len);
            Dump.dump(GroovyServer.originalErr, b, off, len);
        }

        out.write((HEADER_STREAM_ID+": "+streamIdentifier+ "\n").bytes);
        out.write((HEADER_CHUNK_SIZE+": " + len+"\n").bytes);
        out.write("\n".bytes);
        out.write(b, off, len);
    }

    public ChunkedPrintStream(OutputStream outs, char id) {
        super(outs);
        streamIdentifier = id;
    }

}
