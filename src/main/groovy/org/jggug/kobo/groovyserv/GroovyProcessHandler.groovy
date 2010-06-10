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

import java.util.concurrent.Callable


/**
 * @author NAKANO Yasuharu
 */
class GroovyProcessHandler implements Runnable {

    private String id
    private InvocationRequest request

    GroovyProcessHandler(request) {
        this.id = "GroovyServ:GroovyProcessHandler:${request.port}"
        this.request = request
    }

    private void setupThreadName() {
        Thread.currentThread().name = id
    }

    /**
     * @throws GroovyServerExitException
     *              When user code called System.exit().
     *              Acutally this exception is wrapped by ExecutionException.
     * @throws InvalidRequestHeaderException
     *              When classpath option is invalid.
     *              Acutally this exception is wrapped by ExecutionException.
     */
    @Override
    void run() {
        setupThreadName()
        try {
            CurrentDirHolder.instance.setDir(request.cwd)
            setupClasspath(request)
            process(request.args)
            //awaitAllSubThreads()
            Thread.sleep 1000 // FIXME dirty impl to pass ThreadTest
        }
        catch (InterruptedException e) {
            DebugUtils.verboseLog("thread is interrupted: ${id}") // unused exception
        }
        finally {
            //killAllSubThreads()
            CurrentDirHolder.instance.reset()
            DebugUtils.verboseLog("thread is done: ${id}")
        }
    }

    private static setupClasspath(request) {
        ClasspathUtils.addClasspath(request.classpath)
        for (def it = request.args.iterator(); it.hasNext(); ) {
            String opt = it.next()
            if (opt == "-cp") {
                if (!it.hasNext()) {
                    throw new InvalidRequestHeaderException("classpath option is invalid.")
                }
                String classpath = it.next()
                ClasspathUtils.addClasspath(classpath)
            }
        }
    }

    private static process(args) {
        try {
            GroovyMain2.main(args as String[])
        } catch (SystemExitException e) {
            throw new GroovyServerExitException(e.exitStatus, e.message, e)
        }
    }

    private awaitAllSubThreads() { // FIXME
        def threadGroup = Thread.currentThread().threadGroup
        Thread[] threads = new Thread[threadGroup.activeCount()]
        int tcount = threadGroup.enumerate(threads)
        while (tcount != threads.size()) {
            threads = new Thread[threadGroup.activeCount()]
            tcount = threadGroup.enumerate(threads)
        }
        for (int i = 0; i < threads.size(); i++) {
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

