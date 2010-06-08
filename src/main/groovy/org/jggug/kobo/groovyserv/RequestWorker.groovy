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


/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class RequestWorker implements Runnable {

    private static final int THREAD_COUNT = 2

    private ClientConnection conn
    private ThreadGroup threadGroup
    private Executor executor
    private Future future

    RequestWorker(clientConnection, threadGroup) {
        this.conn = clientConnection
        this.threadGroup = threadGroup
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT, new ThreadFactory() {
            Thread newThread(Runnable worker) {
                new Thread(threadGroup, worker, "requestWorker:${conn.socket.port}")
            }
        })
    }

    void start() {
        future = executor.submit(this)
    }

    @Override
    void run() {
        try {
            def request = new InvocationRequest(conn)
            CurrentDirHolder.instance.setDir(request.cwd)
            setupClasspath(request)
            process(request.args)
            ensureAllThreadToStop()
            conn.sendExit(0)
        }
        catch (ExitException e) {
            // GroovyMain2 throws ExitException when it catches ExitException.
            conn.sendExit(e.exitStatus)
        }
        catch (Throwable e) {
            DebugUtils.errLog("unexpected error", e)
            conn.sendExit(1)
        }
        finally {
            CurrentDirHolder.instance.reset()
            conn.close()
        }
    }

    private static setupClasspath(request) {
        ClasspathUtils.addClasspath(request.classpath)
        for (def it = request.args.iterator(); it.hasNext(); ) {
            String opt = it.next()
            if (opt == "-cp") {
                if (!it.hasNext()) {
                    throw new GroovyServerException("classpath option is invalid.")
                }
                String classpath = it.next()
                ClasspathUtils.addClasspath(classpath)
            }
        }
    }

    private static process(args) {
        GroovyMain2.main(args as String[])
    }

    private ensureAllThreadToStop() {
        ThreadGroup tg = Thread.currentThread().threadGroup
        Thread[] threads = new Thread[tg.activeCount()]
        int tcount = tg.enumerate(threads)
        while (tcount != threads.size()) {
            threads = new Thread[tg.activeCount()]
            tcount = tg.enumerate(threads)
        }
        for (int i=0; i<threads.size(); i++) {
            if (threads[i] != Thread.currentThread() && threads[i].isAlive()) {
                if (threads[i].isDaemon()) {
                    threads[i].interrupt()
                }
                else {
                    threads[i].interrupt()
                    threads[i].join()
                }
            }
        }
    }

}

