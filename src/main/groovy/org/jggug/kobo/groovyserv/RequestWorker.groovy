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

import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException
import java.util.concurrent.CancellationException


/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class RequestWorker extends ThreadPoolExecutor {

    private static final int THREAD_COUNT = 3

    private String id
    private ClientConnection conn
    private Future processFuture
    private Future streamFuture
    private Future monitorFuture

    RequestWorker(cookie, socket) {
        // API: ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) 
        super(THREAD_COUNT, THREAD_COUNT, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>())
        this.id = "GroovyServ:RequestWorker:${socket.port}"
        def threadGroup = new ThreadGroup(id) // for stream management
        this.conn = new ClientConnection(cookie, socket, threadGroup)
        setThreadFactory(new ThreadFactory() {
            Thread newThread(Runnable worker) {
                new Thread(threadGroup, worker)
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
            processFuture = submit(new GroovyProcessHandler(request))
            streamFuture  = submit(new StreamRequestHandler(conn))
            monitorFuture = submit(new ExitMonitorHandler())
        } finally {
            // this method call makes a reservation of shutdown a thread pool without directly interrupt.
            // when all tasks will finish, executor will be shut down.
            shutdown()
            DebugUtils.verboseLog("request worker is started: ${id}")
        }
    }

    protected void beforeExecute(Thread thread, Runnable runnable) {
        DebugUtils.verboseLog("beforeExecute: ${thread}: ${runnable['id']}")
    }
    protected void afterExecute(Runnable runnable, Throwable t) {
        DebugUtils.verboseLog("afterExecute: ${runnable['id']}")
    }
    protected void terminated() {
        DebugUtils.verboseLog("terminated: ${id}")
    }

    class ExitMonitorHandler implements Runnable {
        private String id

        ExitMonitorHandler() {
            this.id = "GroovyServ:ExitMonitorHandler:${conn.socket.port}"
        }

        @Override
        void run() {
            Thread.currentThread().name = id
            try {
                IOUtils.awaitFutures([processFuture, streamFuture])
                conn.sendExit(ExitStatus.SUCCESS.code)
            }
            catch (InterruptedException e) {
                DebugUtils.verboseLog("thread is interrupted: ${id}: ${e.message}") // unused details of exception
                conn.sendExit(ExitStatus.INTERRUPTED.code)
            }
            catch (ClientInterruptionException e) {
                DebugUtils.verboseLog("interrupted by client: ${id}: ${e.message}")
                conn.sendExit(e.exitStatus)
            }
            catch (GroovyServerExitException e) {
                DebugUtils.verboseLog("exited: ${id}: ${e.message}")
                conn.sendExit(e.exitStatus)
            }
            catch (GroovyServerException e) {
                DebugUtils.errLog("error: ${id}: ${e.exitStatus}: ${e.message}", e)
                conn.sendExit(e.exitStatus)
            }
            catch (Throwable e) {
                DebugUtils.errLog("unexpected error: ${id}", e)
                conn.sendExit(ExitStatus.UNEXPECTED_ERROR.code)
            }
            finally {
                processFuture.cancel(true)
                streamFuture.cancel(true)
                IOUtils.close(conn)
            }
        }
    }

}

