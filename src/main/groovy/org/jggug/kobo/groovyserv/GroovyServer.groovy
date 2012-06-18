/*
 * Copyright 2009-2011 the original author or authors.
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

/**
 * GroovyServer runs groovy command background.
 * This makes groovy response time at startup very quicker.
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class GroovyServer {

    private static final int DEFAULT_PORT = 1961

    private ServerSocket serverSocket
    private AuthToken authToken

    static void main(String[] args) {
        new GroovyServer().start()
    }

    void start() {
        try {
            WorkFiles.setUp()
            EnvironmentVariables.setUp()
            StandardStreams.setUp()
            setupSecurityManager()
            setupRunningMode()
            startServer()
            setupAuthToken()
            handleRequest()
        }
        catch (GServException e) {
            DebugUtils.errorLog("Error: GroovyServer", e)
            System.exit(e.exitStatus)
        }
        catch (Throwable e) {
            DebugUtils.errorLog("Unexpected error: GroovyServer", e)
            System.exit(ExitStatus.UNEXPECTED_ERROR.code)
        }
    }

    private void setupSecurityManager() {
        System.setSecurityManager(new NoExitSecurityManager2())
    }

    private void setupRunningMode() {
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
        DebugUtils.infoLog "Server started with port: ${port}"
        DebugUtils.infoLog "Default classpath: ${System.getenv('CLASSPATH')}"
    }

    private void handleRequest() {
        while (true) {
            def socket = serverSocket.accept()
            DebugUtils.verboseLog "Accepted socket: ${socket}"
            try {
                // this socket will be closed under a responsibility of RequestWorker
                new RequestWorker(authToken, socket).start()
            } catch (e) {
                DebugUtils.errorLog("Failed to invoke RequestWorker: ${socket}", e)
            }
        }
    }

    static int getPortNumber() {
        return (System.getProperty("groovyserver.port") ?: DEFAULT_PORT) as int
    }
}
