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
class StreamManager {

    static final ORIGINAL = [
        ins: System.in,
        out: System.out,
        err: System.err
    ]

    private static final ALTERNATES = [
        ins: new MultiplexedInputStream(),
        out: ChunkedOutputStream.newOut(),
        err: ChunkedOutputStream.newErr()
    ]

    static void init() {
        System.in  = ALTERNATES.ins
        System.out = new PrintStream(ALTERNATES.out)
        System.err = new PrintStream(ALTERNATES.err)
    }

    static bind(ThreadGroup tg, Socket socket) {
        ALTERNATES.ins.bind(socket.inputStream, tg)
        ALTERNATES.out.bind(socket.outputStream, tg)
        ALTERNATES.err.bind(socket.outputStream, tg)
    }

}

