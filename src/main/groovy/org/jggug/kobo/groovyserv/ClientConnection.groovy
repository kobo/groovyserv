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
 *            <size>==-1 means client exited.
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

    private String id
    private Cookie cookie
    private Socket socket
    private ThreadGroup ownerThreadGroup
    private PipedOutputStream pipedOutputStream // to transfer from socket.inputStream
    private PipedInputStream pipedInputStream   // connected to socket.inputStream indirectly via pipedInputStream

    ClientConnection(cookie, socket, ownerThreadGroup) {
    this.id = "ClientConnection:${socket.port}"
        this.cookie = cookie
        this.socket = socket
        this.ownerThreadGroup = ownerThreadGroup
        this.pipedOutputStream = new PipedOutputStream()
        this.pipedInputStream = new PipedInputStream(pipedOutputStream)
        ClientConnectionRepository.instance.bind(ownerThreadGroup, this)
    }

    /**
     * @throws InvalidRequestHeaderException
     * @throws GroovyServerIOException
     */
    InvocationRequest openSession() {
        InvocationRequest.read(this)
    }

    /**
     * @throws GroovyServerIOException
     */
    void writeFromStreamRequest(byte[] buff, int offset, int result) {
        try {
            pipedOutputStream.write(buff, offset, result)
            pipedOutputStream.flush()
        } catch (InterruptedIOException e) {
            throw new GroovyServerIOException("${id}: I/O interrupted: Failed to write to piped stream", e)
        } catch (IOException e) {
            throw new GroovyServerIOException("${id}: I/O error: Failed to write to piped stream", e)
        }
    }

    /**
     * to return InputStream which you can use as System.in.
     */
    InputStream getInputStream() {
        pipedInputStream
    }

    /**
     * to return InputStream which you can use as System.out or System.err.
     */
    OutputStream getOutputStream() {
        socket.outputStream
    }

    /**
     * @throws GroovyServerIOException
     */
    void sendExit(int status) {
        try {
            socket.outputStream.with { // not to close yet
                write(formatAsExitHeader(status))
                flush()
            }
        } catch (IOException e) {
            throw new GroovyServerIOException("${id}: I/O error: failed to send exit status", e)
        }
    }

    /**
     * To close socket and tear down some relational environment.
     */
    void close() {
        ClientConnectionRepository.instance.unbind(ownerThreadGroup)
        if (pipedInputStream) {
            IOUtils.close(pipedInputStream)
            DebugUtils.verboseLog "${id}: Piped stream closed"
            pipedInputStream = null
        }
        if (socket) {
            // closing output stream because it needs to flush.
            // socket and socket.inputStream are also closed by closing output stream which is gotten from socket.
            IOUtils.close(socket.outputStream)
            DebugUtils.verboseLog "${id}: Socket closed"
            socket = null
        }
    }

    // ----------------------------------------------------------
    // TODO extracted codes about request header into new class

    final static String HEADER_CURRENT_WORKING_DIR = "Cwd"
    final static String HEADER_ARG = "Arg"
    final static String HEADER_CP = "Cp"
    final static String HEADER_STATUS = "Status"
    final static String HEADER_COOKIE = "Cookie"
    final static String HEADER_STREAM_ID = "Channel"
    final static String HEADER_SIZE = "Size"

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

    /**
     * @throws InvalidRequestHeaderException  when headers are invalid
     * @throws GroovyServerIOException        when reading error
     */
    Map<String, List<String>> readHeaders() {
        parseHeaders(socket.inputStream) // read from socket directly
    }

    private Map<String, List<String>> parseHeaders(InputStream ins) { // FIXME
        def headers = [:]
        def line
        while ((line = readLine(ins)) != "") { // until a first empty line
            def tokens = line.split(':', 2)
            if (tokens.size() != 2) {
                throw new InvalidRequestHeaderException("${id}: Found invalid header line: ${line}")
            }
            def (key, value) = tokens
            if (!headers.containsKey(key)) {
                headers[key] = []
            }
            if (value.charAt(0) == ' ') {
                value = value.substring(1)
            }
            headers[key] += value
        }
        DebugUtils.verboseLog "${id}: Parsed headers: ${headers}"
        headers
    }

    private readLine(InputStream is) { // FIXME maybe this is able to be replaced by default API
        try {
            def baos = new ByteArrayOutputStream()
            int ch
            while ((ch = is.read()) != '\n') {
                if (ch == -1) {
                    return baos.toString()
                }
                baos.write((byte) ch)
            }
            return baos.toString()
        } catch (InterruptedIOException e) {
            throw new GroovyServerIOException("${id}: I/O interrupted: interrupted while reading line", e)
        } catch (IOException e) {
            throw new GroovyServerIOException("${id}: I/O error: failed to read line: ${e.message}", e)
        }
    }

    @Override
    String toString() { id }

}

