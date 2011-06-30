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
    private Future invokeFuture
    private Future streamFuture
    private boolean cancelledByClient

    RequestWorker(cookie, socket) {
        // API: ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)
        super(THREAD_COUNT, THREAD_COUNT, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>())
        this.id = "RequestWorker:${socket.port}"

        def rootThreadGroup = new GServThreadGroup("ThreadGroup:${socket.port}")
        this.conn = new ClientConnection(cookie, socket, rootThreadGroup)

        // for management sub threads in invoke handler.
        setThreadFactory(new ThreadFactory() {
            def index = new AtomicInteger(0)
            Thread newThread(Runnable runnable) {
                // giving individual sub thread group for each thread
                // in order to kill invoke handler's sub threads which were started in user scripts.
                def subThreadGroup = new GServThreadGroup(rootThreadGroup, "${rootThreadGroup.name}:${index.getAndIncrement()}")
                new Thread(subThreadGroup, runnable)
            }
        })
    }

    void start() {
        try {
            def request = conn.openSession()
            streamFuture = submit(new StreamRequestHandler(conn))
            invokeFuture = submit(new GroovyInvokeHandler(request))
            DebugUtils.verboseLog("${id}: Request worker is started")

            // when all tasks will finish, executor will be shut down.
            shutdown()
        }
        catch (GServException e) {
            // cancelling all tasks
            if (!isShutdown()) shutdownNow()

            DebugUtils.errorLog("${id}: Failed to start request worker", e)
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

        // to await assign futures to instance variables.
        // if user script is very very short, a thread which is invoking the script
        // will be done quickly before assign to instance variables in start() method.
        while (!streamFuture || !invokeFuture) {
            Thread.sleep 10
        }

        switch (runnable) {
            case invokeFuture:
                DebugUtils.verboseLog("${id}: Invoke handler is dead: ${runnable}", e)
                if (!cancelledByClient) {
                    // connection is maybe shuted down by client, so exit status is not sent.
                    conn.sendExit(getExitStatus(runnable))
                }
                streamFuture.cancel(true)
                closeSafety()
                break
            case streamFuture:
                DebugUtils.verboseLog("${id}: Stream handler is dead: ${runnable}", e)
                cancelledByClient = isCancelledByClient(runnable)
                if (cancelledByClient) {
                    invokeFuture.cancel(true)
                }
                break
            default:
                throw new GServIllegalStateException("${id}: unexpected state: runnable=${runnable}, invokeFuture=${invokeFuture}, streamFuture=${streamFuture}")
        }
    }

    private closeSafety() {
        // if stream handler is blocking to read from input stream,
        // this closing makes socket error, then blocking in stream handler is cancelled.
        IOUtils.close(conn)
        conn = null
    }

    @Override
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
        catch (GServExitException e) {
            DebugUtils.verboseLog("${id}: Exited: ${e.exitStatus}: ${e.message}", e)
            return e.exitStatus
        }
        catch (GServException e) {
            DebugUtils.errorLog("${id}: Error: ${e.exitStatus}", e)
            return e.exitStatus
        }
        catch (Throwable e) {
            DebugUtils.errorLog("${id}: Unexpected error", e)
            return ExitStatus.UNEXPECTED_ERROR.code
        }
    }

    private isCancelledByClient(runnable) {
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
            DebugUtils.errorLog("${id}: Unexpected error", e)
        }
        return false
    }

}

