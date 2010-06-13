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
class GroovyInvokeHandler implements Runnable {

    private static final THREAD_JOIN_TIMEOUT = 1000 // msec

    private String id
    private InvocationRequest request

    GroovyInvokeHandler(request) {
        this.id = "GroovyServ:GroovyInvokeHandler:${request.port}"
        this.request = request
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
        Thread.currentThread().name = id
        try {
            CurrentDirHolder.instance.setDir(request.cwd)
            setupClasspath(request)
            invokeGroovy(request.args)
            awaitAllSubThreads()
            interruptAllSubThreads()
        }
        catch (InterruptedException e) {
            DebugUtils.verboseLog("${id}: Thread interrupted") // unused exception
        }
        finally {
            killAllSubThreads()
            CurrentDirHolder.instance.reset()
            DebugUtils.verboseLog("${id}: Thread is dead")
        }
    }

    @Override
    String toString() { id }

    private static setupClasspath(request) {
        ClasspathUtils.addClasspath(request.classpath)
        for (def it = request.args.iterator(); it.hasNext(); ) {
            String opt = it.next()
            if (opt == "-cp") {
                if (!it.hasNext()) {
                    throw new InvalidRequestHeaderException("Invalid classpath option: ${request.args}")
                }
                String classpath = it.next()
                ClasspathUtils.addClasspath(classpath)
            }
        }
    }

    private static invokeGroovy(args) {
        try {
            GroovyMain2.main(args as String[])
        } catch (SystemExitException e) {
            throw new GroovyServerExitException(e.exitStatus, e.message, e)
        }
    }

    private awaitAllSubThreads() {
        getAllAliveSubThreads().each { thread ->
            if (!thread.isDaemon()) thread.join(THREAD_JOIN_TIMEOUT)
        }
    }

    private interruptAllSubThreads() {
        getAllAliveSubThreads().each { thread ->
            thread.interrupt()
            if (!thread.isDaemon()) thread.join(THREAD_JOIN_TIMEOUT)
        }
    }

    private killAllSubThreads() {
        getAllAliveSubThreads().each { thread ->
            thread.stop() // FORCELY
        }
    }

    private getAllAliveSubThreads() {
        def threadGroup = Thread.currentThread().threadGroup
        Thread[] threads = new Thread[threadGroup.activeCount() + 1] // need at lease one extra space (see Javadoc of ThreadGroup)
        int count = threadGroup.enumerate(threads)
        if (count < threads.size()) {
            // convert to list for convenience except own thread
            def list = (threads as List).findAll{ it && it != Thread.currentThread() }
            DebugUtils.verboseLog("${id}: Found ${list.size()} sub thread(s): ${list}")
            return list
        }
        return getAllAliveSubThreads()
    }

}

