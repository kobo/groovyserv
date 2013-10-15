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

import org.jggug.kobo.groovyserv.exception.GServException
import org.jggug.kobo.groovyserv.platform.EnvironmentVariables
import org.jggug.kobo.groovyserv.stream.StandardStreams
import org.jggug.kobo.groovyserv.utils.DebugUtils

/**
 * GroovyServer runs groovy command background.
 * This makes groovy response time at startup very quicker.
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class GroovyServer {

    static final int DEFAULT_PORT = 1961

    Integer port
    ServerSocket serverSocket
    AuthToken authToken
    List<String> allowFrom = []

    void start() {
        assert port != null
        try {
            // Preparing
            WorkFiles.setUp(port)
            EnvironmentVariables.setUp()
            StandardStreams.setUp()
            setupSecurityManager()
            setupRunningMode()

            // Starting
            startServer()
            setupAuthToken()
            handleRequest()
        }
        catch (GServException e) {
            DebugUtils.errorLog "Error: GroovyServer", e
            exit e.exitStatus
        }
        catch (Throwable e) {
            DebugUtils.errorLog "Unexpected error: GroovyServer", e
            exit ExitStatus.UNEXPECTED_ERROR
        }
        finally {
            authToken.delete()
        }
    }

    void shutdown() {
        authToken.delete()
        DebugUtils.infoLog "Server is shut down"
        exit ExitStatus.FORCELY_SHUTDOWN
    }

    private static exit(ExitStatus status) {
        exit(status.code)
    }

    private static exit(int statusCode) {
        System.setSecurityManager(null)
        System.exit(statusCode)
    }

    private static void setupSecurityManager() {
        System.setSecurityManager(new NoExitSecurityManager2())
    }

    private static void setupRunningMode() {
        // It's an original system property of GroovyServ which is used
        // to identify whether a groovy script is running on normal groovy or groovyserv.
        System.setProperty("groovy.runningmode", "server")
    }

    private void setupAuthToken() {
        if (authToken == null) {
            authToken = new AuthToken()
        }
        authToken.save()
    }

    private void startServer() {
        serverSocket = new ServerSocket(port)
        DebugUtils.infoLog "Server is started with ${port} port" + (allowFrom ? " allowing from ${allowFrom.join(" and ")}" : "")
        DebugUtils.infoLog "Default classpath: ${System.getenv('CLASSPATH')}"
    }

    private void handleRequest() {
        while (true) {
            def socket = serverSocket.accept()
            DebugUtils.verboseLog "Accepted socket: ${socket}"

            // This socket must be closed under a responsibility of RequestWorker.
            // RequestWorker must be invoked on the new thread in order to apply new thread group to all sub threads.
            // It's because ClientConnection is managed for each thread by InheritableThreadLocal.
            newRequestWorker(socket).start()
        }
    }

    private Thread newRequestWorker(Socket socket) {
        def threadGroup = new GServThreadGroup("GServThreadGroup:${socket.port}")
        new Thread(threadGroup, new RequestWorker(authToken, socket), "RequestWorker:${socket.port}")
    }
}
