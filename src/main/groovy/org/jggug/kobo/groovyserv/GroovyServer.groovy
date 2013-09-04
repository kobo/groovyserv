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
@Singleton
class GroovyServer {

    private static final int DEFAULT_PORT = 1961

    private ServerSocket serverSocket
    private AuthToken authToken

    static void main(String[] args) {
        GroovyServer.instance.start()
    }

    void start() {
        try {
            WorkFiles.setUp()
            EnvironmentVariables.setUp()
            StandardStreams.setUp()
            setupSecurityManager()
            setupRunningMode()

            // The order is important.
            // groovyclient uses a port for checking whether server is available.
            // If a port was opened before creating authtoken, checking availability was passed.
            // But in too slow machine, client may access an authtoken file before it's created.
            // So an authtoken file should be created before a port is opened.
            setupAuthToken()
            startServer()

            handleRequest()
        }
        catch (GServException e) {
            DebugUtils.errorLog("Error: GroovyServer", e)
            System.exit(e.exitStatus)
        }
        catch (Throwable e) {
            DebugUtils.errorLog("Unexpected error: GroovyServer", e)
            System.exit(ExitStatus.UNEXPECTED_ERROR.code)
        } finally {
            authToken.delete()
        }
    }

    void shutdown() {
        authToken.delete()
        DebugUtils.infoLog "Server is shut down"
        System.setSecurityManager(null)
        System.exit(ExitStatus.FORCELY_SHUTDOWN.code)
    }

    private void setupSecurityManager() {
        System.setSecurityManager(new NoExitSecurityManager2())
    }

    private void setupRunningMode() {
        // It's an original system property of GroovyServ which is used
        // to identify whether a groovy script is running on normal groovy or groovyserv.
        System.setProperty("groovy.runningmode", "server")
    }

    private void setupAuthToken() {
        def givenAuthToken = System.getProperty("groovyserver.authtoken")
        authToken = new AuthToken(givenAuthToken)
        authToken.save()
    }

    private void startServer() {
        int port = getPortNumber()
        serverSocket = new ServerSocket(port)
        DebugUtils.infoLog "Server is started with port: ${port}"
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

    static int getPortNumber() {
        return (System.getProperty("groovyserver.port") ?: DEFAULT_PORT) as int
    }
}
