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

import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException
import java.util.concurrent.CancellationException

import static java.util.concurrent.TimeUnit.*


/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class RequestWorker {

    private static final int THREAD_COUNT = 3

    private ClientConnection conn
    private Executor executor
    private Future processFuture
    private Future streamFuture
    private Future monitorFuture

    RequestWorker(cookie, socket) {
        def threadGroup = new ThreadGroup("GroovyServ:${socket.port}") // for stream management
        this.conn = new ClientConnection(cookie, socket, threadGroup)
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT, new ThreadFactory() {
            Thread newThread(Runnable worker) {
                new Thread(threadGroup, worker) // TODO it needs cancellable sub-class of Thread class
            }
        })
    }

    /**
     * @throws GroovyServerException when request headers are invalid
     * @throws IOException when failed to read from socket
     */
    void start() {
        try {
            def request = conn.openSession()
            processFuture = executor.submit(new GroovyProcessHandler(request))
            streamFuture  = executor.submit(new StreamRequestHandler(conn))
            monitorFuture = executor.submit(new ExitMonitorHandler())
        } finally {
            // this method call makes a reservation of shutdown a thread pool without directly interrupt.
            // when all tasks will finish, executor will be shut down.
            executor.shutdown()
        }
    }

    void stop() {
        monitorFuture.cancel(true)
    }

    class ExitMonitorHandler implements Runnable {
        private String id

        ExitMonitorHandler() {
            this.id = "GroovyServ:ExitMonitorHandler:${conn.socket.port}"
        }

        private void setupThreadName() {
            Thread.currentThread().name = id
        }

        @Override
        void run() {
            setupThreadName()
            try {
                IOUtils.awaitFutures([processFuture, streamFuture])
                conn.sendExit(0)
            }
            catch (InterruptedException e) {
                DebugUtils.verboseLog("thread is interrupted: ${id}: ${e.message}") // unused details of exception
                conn.sendExit(1)
            }
            catch (ExitException e) {
                DebugUtils.verboseLog("worker thread exited: ${e.exitStatus}: ${id}: ${e.message}") // unused details of exception
                conn.sendExit(e.exitStatus)
            }
            catch (Throwable e) {
                DebugUtils.errLog("unexpected error: ${id}", e)
                conn.sendExit(2)
            }
            finally {
                processFuture.cancel(true)
                streamFuture.cancel(true)
                IOUtils.close(conn)
            }
        }
    }

}

