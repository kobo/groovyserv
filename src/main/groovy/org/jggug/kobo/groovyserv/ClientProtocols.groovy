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
package org.jggug.kobo.groovyserv

import org.jggug.kobo.groovyserv.exception.GServIOException
import org.jggug.kobo.groovyserv.exception.InvalidAuthTokenException
import org.jggug.kobo.groovyserv.exception.InvalidRequestHeaderException
import org.jggug.kobo.groovyserv.utils.DebugUtils
import org.jggug.kobo.groovyserv.utils.IOUtils

/**
 * Protocol summary:
 * <pre>
 * Request ::= InvocationRequest
 *             ( StreamRequest ) *
 * Response ::= ( StreamResponse ) *
 *
 * InvocationRequest ::=
 *    'Protocol:' <protocol> LF
 *    'Cwd:' <cwd> LF
 *    'Arg:' <arg1> LF
 *    'Arg:' <arg2> LF
 *      :
 *    'Arg:' <argN> LF
 *    'Env:' <env1>=<value1> LF
 *    'Env:' <env2>=<value2> LF
 *      :
 *    'Env:' <envN>=<valueN> LF
 *    'Cp:' <classpath> LF
 *    'Auth:' <authToken> LF
 *    'Cmd:' <cmd> LF
 *    LF
 *
 *   where:
 *     <protocol> is a type of protocol, like 'simple'. (optional)
 *     <cwd> is current working directory. (optional)
 *     <arg1>,<arg2>..<argN> are commandline arguments which must be encoded by Base64. (optional)
 *     <env1>,<env2>..<envN> are environment variable names which sent to the server. (optional)
 *     <value1>,<value2>..<valueN> are environment variable values which sent to the server. (optional)
 *     <classpath> is the value of environment variable CLASSPATH. (optional)
 *     <authToken> is authentication value which a request is from a valid user who invoked the server. (required)
 *     <cmd> is a command to operate a server from client via port. (optional)
 *     LF is line feed (0x0a, '\n').
 *
 * StreamRequest ::=
 *    'Size:' <size> LF
 *    LF
 *    <body from STDIN>
 *
 *   where:
 *     <size> is the size of body to send to server.
 *            <size>==-1 means client exited.
 *     <body from STDIN> is byte sequence from standard input.
 *
 * StreamResponse ::=
 *    'Channel:' <id> LF
 *    'Size:' <size> LF
 *    LF
 *    <body for STDERR/STDOUT>
 *
 *   where:
 *     <id> is 'out' or 'err', where 'out' means standard output of the program.
 *          'err' means standard error of the program.
 *     <size> is the size of chunk.
 *     <body from STDERR/STDOUT> is byte sequence from standard output/error.
 *
 * InvocationResponse ::=
 *    'Status:' <status> LF
 *
 *   where:
 *     <status> is exit status of invoked groovy script.
 *
 * </pre>
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class ClientProtocols {

    private final static String HEADER_CURRENT_WORKING_DIR = "Cwd"
    private final static String HEADER_ARG = "Arg"
    private final static String HEADER_CP = "Cp"
    private final static String HEADER_STATUS = "Status"
    private final static String HEADER_AUTHTOKEN = "Auth"
    private final static String HEADER_STREAM_ID = "Channel"
    private final static String HEADER_SIZE = "Size"
    private final static String HEADER_ENV = "Env"
    private final static String HEADER_PROTOCOL = "Protocol"
    private final static String HEADER_COMMAND = "Cmd"
    private final static String LINE_SEPARATOR = "\n"

    /**
     * @throws InvalidAuthTokenException
     * @throws InvalidRequestHeaderException
     * @throws GServIOException
     */
    static InvocationRequest readInvocationRequest(ClientConnection conn) {
        def id = "${ClientProtocols.simpleName}:${conn.socket.port}"
        Map<String, List<String>> headers = readHeaders(conn)
        def request = new InvocationRequest(
            port: conn.socket.port,
            cwd: headers[HEADER_CURRENT_WORKING_DIR]?.getAt(0),
            classpath: headers[HEADER_CP]?.getAt(0),
            args: decodeArgs(id, headers[HEADER_ARG]),
            clientAuthToken: headers[HEADER_AUTHTOKEN]?.getAt(0),
            serverAuthToken: conn.authToken,
            envVars: headers[HEADER_ENV],
            protocol: headers[HEADER_PROTOCOL]?.getAt(0),
            command: headers[HEADER_COMMAND]?.getAt(0),
        )
        request.check()
        return request
    }

    private static List<String> decodeArgs(String id, List<String> encoded) {
        encoded.collect {
            try {
                new String(it.decodeBase64()) // using default encoding
            } catch (RuntimeException e) {
                throw new InvalidRequestHeaderException("${id}: Found invalid arguments: ${it}", e)
            }
        }
    }

    /**
     * @throws InvalidRequestHeaderException
     * @throws GServIOException
     */
    static StreamRequest readStreamRequest(ClientConnection conn) {
        Map<String, List<String>> headers = readHeaders(conn)
        def request = new StreamRequest(
            port: conn.socket.port,
            size: headers[HEADER_SIZE]?.getAt(0),
            command: headers[HEADER_COMMAND]?.getAt(0),
        )
        request.check()
        return request
    }

    private static Map<String, List<String>> readHeaders(ClientConnection conn) {
        def id = "${ClientProtocols.simpleName}:${conn.socket.port}"
        def ins = conn.socket.inputStream // raw stream
        return parseHeaders(id, ins)
    }

    static Map<String, List<String>> parseHeaders(String id, InputStream ins) {
        try {
            def headers = [:]
            IOUtils.readLines(ins).each { String line ->
                def tokens = line.split(':', 2)
                def key = tokens[0] // exists at least
                def value = (tokens.size() > 1) ? tokens[1] : ''
                headers.get(key, []) << value.trim()
            }
            DebugUtils.verboseLog id, """Parsed headers: ${
                headers.collectEntries { key, value -> [key, (key == HEADER_AUTHTOKEN) ? '*' * 8 : value] }
            }"""
            return headers
        }
        catch (InterruptedIOException e) {
            throw new GServIOException("${id}: I/O interrupted: interrupted while reading line", e)
        }
        catch (IOException e) {
            throw new GServIOException("${id}: I/O error: failed to read line: ${e.message}", e)
        }
    }

    static byte[] readBody(InputStream inputStream, int size) {
        def buff = new byte[size]
        int offset = 0
        int result = inputStream.read(buff, offset, size) // read from raw stream
        assert size == result
        return buff
    }

    static byte[] formatAsResponseHeader(streamId, size) {
        def header = [:]
        header[HEADER_STREAM_ID] = streamId
        header[HEADER_SIZE] = size
        formatAsHeader(header)
    }

    static byte[] formatAsExitHeader(int status, String body = null) {
        def header = [:]
        header[HEADER_STATUS] = status
        formatAsHeader(header, body)
    }

    private static byte[] formatAsHeader(Map map, String body = null) {
        def buff = new StringBuilder()
        map.each { key, value ->
            if (key) {
                if (value instanceof Collection) {
                    value.each { v ->
                        buff << "$key: $v" << LINE_SEPARATOR
                    }
                } else {
                    buff << "$key: $value" << LINE_SEPARATOR
                }
            }
        }
        buff << LINE_SEPARATOR
        if (body) {
            buff << body
        }
        buff.toString().bytes
    }

}

