#!/usr/bin/env groovy
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

import org.codehaus.groovy.tools.shell.util.NoExitSecurityManager


/**
 * GroovyServer runs groovy command background.
 * This makes groovy response time at startup very quicker.
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class GroovyServer {

    private static final int DEFAULT_PORT = 1961
    private static final String PROPS_KEY_PORT = "groovyserver.port"

    static void main(String[] args) {
        new GroovyServer().start()
    }

    void start() {
        try {
            setupStandardStreams()
            setupSecurityManager()
            setupRunningMode()
            runServer()
        }
        catch (GroovyServerException e) {
            DebugUtils.errLog e.message
            System.exit(1)
        }
    }

    private void setupStandardStreams() {
        StreamManager.init()
    }

    private void setupSecurityManager() {
        System.setSecurityManager(new NoExitSecurityManager2()) // TODO partially override
    }

    private void setupRunningMode() {
        System.setProperty("groovy.runningmode", "server")
    }

    private void runServer() {
        def cookie = CookieUtils.createCookie()

        def serverSocket = new ServerSocket(getPort())
        while (true) {
            def socket = serverSocket.accept()
            if (!socket.localSocketAddress.address.isLoopbackAddress()) { // for security
                DebugUtils.errLog "allow connection from loopback address only"
                continue
            }
            DebugUtils.verboseLog "accepted socket=$socket"

            // Create new thread for each connections.
            // Here, don't use ExecutorService or any thread pool system.
            // Because the System.(in/out/err) streams are used distinctly
            // by thread instance. In the other words, threads can't be pooled.
            // So this 'new Thread()' is necessary.
            def tgroup = new ThreadGroup("groovyserver:$socket")
            def connection = new ClientConnection(cookie, socket, tgroup)
            new Thread(tgroup, new RequestWorker(connection), "requestWorker").start()
        }
    }

    private static int getPort() {
        if (System.getProperty(PROPS_KEY_PORT) != null) {
            return System.getProperty(PROPS_KEY_PORT) as int
        }
        return DEFAULT_PORT
    }
}

