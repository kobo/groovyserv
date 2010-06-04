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
 * Protocol summary:
 * <pre>
 * Request ::= InvocationRequest
 *             ( StreamRequest ) *
 * Response ::= ( StreamResponse ) *
 *
 * InvocationRequest ::=
 *    'Cwd:' <cwd> LF
 *    'Arg:' <argn> LF
 *    'Arg:' <arg1> LF
 *    'Arg:' <arg2> LF
 *    'Cp:' <classpath> LF
 *    'Cookie:' <cookie> LF
 *    LF
 *
 *   where:
 *     <cwd> is current working directory.
 *     <arg1><arg2>.. are commandline arguments(optional).
 *     <classpath>.. is the value of environment variable CLASSPATH(optional).
 *     <cookie> is authentication value which certify client is the user who
 *              invoked the server.
 *     LF is carridge return (0x0d ^M) and line feed (0x0a, '\n').
 *
 * StreamRequest ::=
 *    'Size:' <size> LF
 *    LF
 *    <data from STDIN>
 *
 *   where:
 *     <size> is the size of data to send to server.
 *            <size>==0 means client exited.
 *     <data from STDIN> is byte sequence from standard input.
 *
 * StreamResponse ::=
 *    'Status:' <status> LF
 *    'Channel:' <id> LF
 *    'Size:' <size> LF
 *    LF
 *    <data for STDERR/STDOUT>
 *
 *   where:
 *     <status> is exit status of invoked groovy script.
 *     <id> is 'o' or 'e', where 'o' means standard output of the program.
 *          'e' means standard error of the program.
 *     <size> is the size of chunk.
 *     <data from STDERR/STDOUT> is byte sequence from standard output/error.
 *
 * </pre>
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class ClientConnection implements Closeable {

    final static String HEADER_CURRENT_WORKING_DIR = "Cwd"
    final static String HEADER_ARG = "Arg"
    final static String HEADER_CP = "Cp"
    final static String HEADER_STATUS = "Status"
    final static String HEADER_COOKIE = "Cookie"
    final static String HEADER_STREAM_ID = "Channel"
    final static String HEADER_SIZE = "Size"

    private Cookie cookie
    private Socket socket
    private ThreadGroup ownerThreadGroup

    ClientConnection(cookie, socket, ownerThreadGroup) {
        this.cookie = cookie
        this.socket = socket
        this.ownerThreadGroup = ownerThreadGroup
        ClientConnectionRepository.instance.bind(ownerThreadGroup, this)
    }

    Map<String, List<String>> readHeaders() {
        parseHeaders(socket.inputStream)
    }

    void sendExit(int status) {
        socket.outputStream.with { // not to close yet
            write(ClientConnection.formatAsExitHeader(status))
            flush()
        }
    }

    void close() {
        if (socket) {
            // because output stream need flush.
            // socket is also closed by closing output stream which is gotten from socket.
            socket.outputStream.close()
            DebugUtils.verboseLog "client connection is closed: ${socket.port}"
            socket = null
        }
        ClientConnectionRepository.instance.unbind(ownerThreadGroup)
    }

    static byte[] formatAsResponseHeader(streamId, size) {
        def header = [:]
        header[HEADER_STREAM_ID] = streamId as char
        header[HEADER_SIZE] = size
        formatAsHeader(header)
    }

    private static byte[] formatAsExitHeader(status) {
        def header = [:]
        header[HEADER_STATUS] = status
        formatAsHeader(header)
    }

    private static byte[] formatAsHeader(map) {
        def buff = new StringBuilder()
        map.each { key, value ->
            if (key) {
                buff << "$key: $value\n"
            }
        }
        buff << "\n"
        buff.toString().bytes
    }

    private static Map<String, List<String>> parseHeaders(InputStream ins) {
        def headers = [:]
        def line
        while ((line = readLine(ins)) != "") {
            def kv = line.split(':', 2)
            def key = kv[0]
            def value = kv[1]
            if (!headers.containsKey(key)) {
                headers[key] = []
            }
            if (value.charAt(0) == ' ') {
                value = value.substring(1)
            }
            headers[key] += value
        }
        if (DebugUtils.isVerboseMode()) {
            DebugUtils.errLog "parsed headers: " + headers.collect { k, v -> "$k = $v" }.join(", ")
        }
        headers
    }

    private static readLine(InputStream is) { // FIXME maybe this is able to be replaced by default API
        def baos = new ByteArrayOutputStream()
        int ch
        while ((ch = is.read()) != '\n') {
            if (ch == -1) {
                return baos.toString()
            }
            baos.write((byte) ch)
        }
        return baos.toString()
    }

}

