/*
 * Copyright 2009-2013 the original author or authors.
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
package org.jggug.kobo.groovyserv.stream

import org.jggug.kobo.groovyserv.ClientConnection

/**
 * @author NAKANO Yasuharu
 */
class StandardStreams {

    static final ORIGINAL = [
        ins: System.in,
        out: System.out,
        err: System.err
    ]

    private static final ALTERNATES = [
        ins: newInAsInputStream(),
        out: newOutAsPrintStream(),
        err: newErrAsPrintStream(),
    ]

    static void setUp() {
        // The standard streams are replaced with GroovyServ's ones
        // which can handle the socket for each request thread.
        System.in = ALTERNATES.ins
        System.out = ALTERNATES.out
        System.err = ALTERNATES.err
    }

    private static InputStream newInAsInputStream() {
        new DynamicDelegatedInputStream({-> ClientConnection.currentConnection.ins })
    }

    private static PrintStream newOutAsPrintStream() {
        new DynamicDelegatedPrintStream({-> ClientConnection.currentConnection.out })
    }

    private static PrintStream newErrAsPrintStream() {
        new DynamicDelegatedPrintStream({-> ClientConnection.currentConnection.err })
    }

}

