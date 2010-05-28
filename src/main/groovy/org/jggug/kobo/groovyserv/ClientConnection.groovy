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
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class ClientConnection {

    final static String HEADER_CURRENT_WORKING_DIR = "Cwd"
    final static String HEADER_ARG = "Arg"
    final static String HEADER_CP = "Cp"
    final static String HEADER_STATUS = "Status"
    final static String HEADER_COOKIE = "Cookie"
    final static String HEADER_STREAM_ID = "Channel"
    final static String HEADER_SIZE = "Size"

    private cookie
    private socket
    private ownerThreadGroup

    ClientConnection(cookie, socket, ownerThreadGroup) {
        this.cookie = cookie
        this.socket = socket
        this.ownerThreadGroup = ownerThreadGroup
        StreamManager.bind(ownerThreadGroup, socket)
    }

    Map<String, List<String>> readHeaders() {
        def headers = readHeaders(socket.inputStream)
        checkHeaders(headers, cookie)
        headers
    }

    void sendExit(int status) {
        socket.outputStream.with { // not to close yet
            write(Protocol.formatAsExitHeader(status))
            flush()
        }
    }

    void close() {
        if (!socket) return
        socket.close()
        socket = null
//        StreamManager.unbind(thread)
    }

    String toString() {
        "ClientConnection: ${socket.port}"
    }

    static byte[] formatAsResponseHeader(streamId, size) {
        def header = [:]
        header[HEADER_STREAM_ID] = streamId
        header[HEADER_SIZE] = size
        formatAsHeader(header)
    }

    private static byte[] formatAsExitHeader(status) {
        def header = [:]
        header[HEADER_STATUS] = status
        formatAsHeader(header)
    }

    private static checkHeaders(headers, cookie) {
        if (headers[HEADER_CURRENT_WORKING_DIR] == null || headers[HEADER_CURRENT_WORKING_DIR][0] == null) {
            throw new GroovyServerException("required header cwd unspecified.")
        }
        if (cookie == null || headers[HEADER_COOKIE] == null || headers[HEADER_COOKIE][0] != cookie) {
            Thread.sleep(5000)
            throw new GroovyServerException("authentication failed.")
        }
    }

    private static Map<String, List<String>> readHeaders(InputStream ins) {
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
            headers.each { k,v ->
                DebugUtils.errLog " $k = $v"
            }
        }
        headers
    }

    private static readLine(InputStream is) {
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

}

