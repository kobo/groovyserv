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
import java.util.concurrent.TimeUnit
import java.util.concurrent.Future
import java.util.concurrent.CancellationException
import java.util.concurrent.RunnableFuture
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class RequestWorker extends ThreadPoolExecutor {

    private static final int THREAD_COUNT = 2

    private String id
    private ClientConnection conn
    private Future processFuture
    private Future streamFuture

    RequestWorker(cookie, socket) {
        // API: ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) 
        super(THREAD_COUNT, THREAD_COUNT, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>())
        this.id = "GroovyServ:RequestWorker:${socket.port}"

        def threadGroup = new ThreadGroup("GroovyServ:ThreadGroup:${socket.port}")
        this.conn = new ClientConnection(cookie, socket, threadGroup)

        // for management sub threads in process handler.
        setThreadFactory(new ThreadFactory() {
            def index = new AtomicInteger(0)
            Thread newThread(Runnable runnable) {
                // giving individual sub thread group for each thread
                // in order to kill process handler's sub threads which were started in user scripts.
                def subThreadGroup = new ThreadGroup(threadGroup, "GroovyServ:ThreadGroup:${socket.port}:${index.getAndIncrement()}")
                new Thread(subThreadGroup, runnable)
            }
        })
    }

    void start() {
        try {
            def request = conn.openSession()
            processFuture = submit(new GroovyInvokeHandler(request))
            streamFuture  = submit(new StreamRequestHandler(conn))

            // when all tasks will finish, executor will be shut down.
            shutdown()

            DebugUtils.verboseLog("${id}: Request worker is started")
        }
        catch (GroovyServerException e) {
            // cancelling all tasks
            if (!isShutdown()) shutdownNow()

            DebugUtils.errLog "Failed to invoke request worker: ${conn.socket}", e
        }
    }

    @Override
    protected RunnableFuture newTaskFor(Runnable runnable, defaultValue) {
        DebugUtils.verboseLog("${id}: Future task of handler is created: ${runnable.id}")
        new FutureTask(runnable, defaultValue) {
            String toString() { runnable.id } // for debug
        }
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable e) {
        DebugUtils.verboseLog("${id}: Handler is dead: ${runnable}", e)
        super.afterExecute(runnable, e)

        if (runnable == processFuture) {
            DebugUtils.verboseLog("${id}: Process handler is dead: ${runnable}", e)
            conn.sendExit(getExitStatus(runnable))
            streamFuture.cancel(true)
            closeSafety()
        }
        if (runnable == streamFuture) {
            DebugUtils.verboseLog("${id}: Stream handler is dead: ${runnable}", e)
            if (isNeedToCancelProcess(runnable)) {
                processFuture.cancel(true)
            }
        }
    }

    private closeSafety() {
        // if stream handler is blocking to read from input stream, 
        // this closing makes socket error, then blocking in stream handler is cancelled.
        IOUtils.close(conn)
        conn = null
    }

    protected void terminated() {
        closeSafety() // by way of precaution
        DebugUtils.verboseLog("${id}: Terminated")
    }

    private int getExitStatus(runnable) {
        try {
            IOUtils.awaitFuture(runnable)
            return ExitStatus.SUCCESS.code
        }
        catch (CancellationException e) {
            DebugUtils.verboseLog("${id}: Cancelled: ${e.message}")
            return ExitStatus.INTERRUPTED.code
        }
        catch (InterruptedException e) {
            DebugUtils.verboseLog("${id}: Interrupted: ${e.message}")
            return ExitStatus.INTERRUPTED.code
        }
        catch (GroovyServerExitException e) {
            DebugUtils.verboseLog("${id}: Exited: ${e.exitStatus}: ${e.message}")
            return e.exitStatus
        }
        catch (GroovyServerException e) {
            DebugUtils.errLog("${id}: Error: ${e.exitStatus}", e)
            return e.exitStatus
        }
        catch (Throwable e) {
            DebugUtils.errLog("${id}: Unexpected error", e)
            return ExitStatus.UNEXPECTED_ERROR.code
        }
    }

    private isNeedToCancelProcess(runnable) {
        try {
            IOUtils.awaitFuture(runnable)
        }
        catch (ClientInterruptionException e) {
            DebugUtils.verboseLog("${id}: Interrupted by client: ${e.message}")
            return true
        }
        catch (CancellationException e) {
            DebugUtils.verboseLog("${id}: Cancelled: ${e.message}")
        }
        catch (InterruptedException e) {
            DebugUtils.verboseLog("${id}: Interrupted: ${e.message}")
        }
        catch (Throwable e) {
            DebugUtils.errLog("${id}: Unexpected error", e)
        }
        return false
    }

}

