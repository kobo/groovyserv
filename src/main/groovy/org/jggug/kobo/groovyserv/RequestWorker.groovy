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

    private static final int POOL_SIZE = 2

    private String id
    private ClientConnection conn
    private Future invokeFuture
    private Future streamFuture

    RequestWorker(authToken, socket) {
        // API: ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)
        super(POOL_SIZE, POOL_SIZE, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>())
        this.id = "RequestWorker:${socket.port}"

        def rootThreadGroup = new GServThreadGroup("GServThreadGroup:${socket.port}")
        this.conn = new ClientConnection(authToken, socket, rootThreadGroup)

        // for management sub threads in invoke handler.
        setThreadFactory(new ThreadFactory() {
            def index = new AtomicInteger(0)

            Thread newThread(Runnable runnable) {
                // giving individual sub thread group for each thread
                // in order to kill invoke handler's sub threads which were started in user scripts.
                def subThreadGroup = new GServThreadGroup(rootThreadGroup, "${rootThreadGroup.name}:${index.getAndIncrement()}")
                def thread = new Thread(subThreadGroup, runnable)
                DebugUtils.verboseLog("${id}: Thread is created: $thread")
                return thread
            }
        })
    }

    void start() {
        def request
        try {
            request = conn.openSession()
        } catch (e) {
            conn.sendExit(e.exitStatus, e.message)
            throw e
        }
        try {
            streamFuture = submit(new StreamRequestHandler(conn))
            invokeFuture = submit(new GroovyInvokeHandler(request))
            DebugUtils.verboseLog("${id}: Request worker is started")

            // when all tasks will finish, executor will be shut down.
            shutdown()
        } catch (GServException e) {
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

        // To await assign futures to instance variables.
        // If user script is very very short, a thread which is invoking the script
        // will be done quickly before assign to instance variables in start() method.
        while (!streamFuture || !invokeFuture) {
            Thread.sleep 10
        }

        int exitStatus = getExitStatus(runnable)
        def anotherFuture = (runnable == invokeFuture) ? streamFuture : invokeFuture
        if (anotherFuture.isDone()) {
            DebugUtils.verboseLog("${id}: Another handler ${anotherFuture} is already done", e)
        } else {
            DebugUtils.verboseLog("${id}: Another handler ${anotherFuture} is canceling by ${runnable}", e)
            anotherFuture.cancel(true)
        }

        // When this executor instance is terminated, the terminated() method is called and in there closeSafety() is called too.
        // If you want to close the socket certainly, it's enough.
        // But if you want to terminate threads of handler, it isn't enough.
        //
        // Calling Future#cancel(true) isn't a certain way to stop a running thread to invoke a user script
        // because the script may not be able to react to an thread's interruption.
        // So Socket#close is called here in order to force to fail reading/writing of standard streams.
        // It causes a termination of the running thread, so certainly the invoke handler is terminated.
        //
        // When the invoke handler ends before the stream handler, the following closeSafety() is also called.
        // It causes IOException on the thread of the stream handler. As a result, the stream handler is also terminated.
        closeSafety(exitStatus)
    }

    private synchronized closeSafety(int exitStatus, String message = null) {
        // While stream handler is blocking to read from the input stream,
        // this closing makes a socket error, and then blocking in stream handler is cancelled.
        if (!conn) return
        try {
            conn.sendExit(exitStatus)
            DebugUtils.verboseLog("${id}: Sent exit status: ${exitStatus}: ${message}")
        } catch (GServIOException e) {
            DebugUtils.verboseLog("${id}: Failed to send exit status: ${exitStatus}: ${message}", e)
        }
        IOUtils.close(conn)
        conn = null
    }

    @Override
    protected void terminated() {
        closeSafety(ExitStatus.TERMINATED.code) // by way of precaution
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
            DebugUtils.verboseLog("${id}: Interrupted as thread: ${e.message}")
            return ExitStatus.INTERRUPTED.code
        }
        catch (GServInterruptedException e) {
            DebugUtils.verboseLog("${id}: Interrupted by client: ${e.message}")
            return ExitStatus.INTERRUPTED.code
        }
        catch (SystemExitException e) {
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
}

