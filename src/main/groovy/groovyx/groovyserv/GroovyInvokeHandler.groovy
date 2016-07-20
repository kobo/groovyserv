/*
 * Copyright 2009 the original author or authors.
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
package groovyx.groovyserv

import groovyx.groovyserv.exception.GServIllegalStateException
import groovyx.groovyserv.exception.InvalidRequestHeaderException
import groovyx.groovyserv.platform.EnvironmentVariables
import groovyx.groovyserv.platform.GlobalCurrentDir
import groovyx.groovyserv.utils.LogUtils

/**
 * @author NAKANO Yasuharu
 */
class GroovyInvokeHandler implements Runnable {

    private static final long TIMEOUT_FOR_JOINING_SUBTHREADS = 1000 // sec
    private static final List<String> CLASSPATH_OPTIONS = ["--classpath", "-cp", "-classpath"]

    private InvocationRequest request
    private boolean interrupted = false

    GroovyInvokeHandler(InvocationRequest request) {
        this.request = request
    }

    /**
     * @throws SystemExitException
     *              When user code called System.exit().
     *              Actually this exception is wrapped by ExecutionException.
     * @throws InvalidRequestHeaderException
     *              When classpath option is invalid.
     *              Actually this exception is wrapped by ExecutionException.
     * @throws GServIllegalStateException
     *              When current directory is changed after different directory is set by another session
     */
    @Override
    void run() {
        Thread.currentThread().name = "Thread:${GroovyInvokeHandler.simpleName}"
        LogUtils.debugLog "Thread started"
        try {
            if (request.cwd) {
                if (!request.shouldKeepServerCwd()) {
                    LogUtils.debugLog "Use request CWD globally: server.cwd=${GlobalCurrentDir.ORIGINAL_PWD}, requet.cwd=${request.resolvedCwd}"
                    GlobalCurrentDir.instance.setDir(request.resolvedCwd)
                } else {
                    LogUtils.debugLog "Use server CWD globally: server.cwd=${GlobalCurrentDir.ORIGINAL_PWD}, requet.cwd=${request.resolvedCwd}"
                }
            }
            setupEnvVars(request.envVars)
            String classpath = removeClasspathFromArgs(request)
            invokeGroovy(request.args, classpath)
            awaitAllSubThreads()
        }
        catch (InterruptedException e) {
            interrupted = true
            LogUtils.debugLog "Thread interrupted: ${e.message}"
        }
        catch (GServIllegalStateException e) {
            throw e
        }
        catch (RuntimeException e) { // TODO using GServInterruptedException
            if (e.cause instanceof InterruptedException) {
                interrupted = true
                LogUtils.debugLog "Thread interrupted: ${e.message}"
            }
            throw e
        }
        finally {
            killAllSubThreadsIfExist()
            if (!request.shouldKeepServerCwd()) {
                GlobalCurrentDir.instance.reset()
            }
            LogUtils.debugLog "Thread is dead"
        }
    }

    private void setupEnvVars(List<String> envVars) {
        envVars.each { envVar ->
            LogUtils.debugLog "putenv(${envVar})"
            EnvironmentVariables.instance.put(envVar)
        }
    }

    /**
     * Setting a classpath using the -cp or -classpath option means not to use the global classpath.
     * GroovyServ behaves then the same as the java interpreter and Groovy.
     */
    private removeClasspathFromArgs(InvocationRequest request) {
        def paths = [] as LinkedHashSet

        // parse classpath option's values from arguments.
        def filteredArgs = [] // args except options about classpath
        for (def it = request.args.iterator(); it.hasNext();) {
            String opt = it.next()
            if (CLASSPATH_OPTIONS.contains(opt)) {
                if (!it.hasNext()) {
                    throw new InvalidRequestHeaderException("Invalid classpath option: ${request.args}")
                }
                paths += it.next().split(File.pathSeparator) as List
            } else {
                filteredArgs << opt
            }
        }

        // if no classpath option, using Cp header from CLASSPATH environment variable on client.
        if (paths.empty && request.classpath) {
            paths = request.classpath.split(File.pathSeparator) as List
        }

        // CWD must be always the last entry of classpath
        paths << "."

        // removed classpath options
        request.args = filteredArgs

        return paths.join(File.pathSeparator)
    }

    private invokeGroovy(List<String> args, String classpath) {
        LogUtils.debugLog "Invoking groovy: ${args} with classpath=${classpath}"
        GroovyMain2.processArgs(args as String[], System.out, classpath)
        appendServerVersion(args)
    }

    private appendServerVersion(List<String> args) {
        if (args.any { it.startsWith("-v") } || args.contains("--version")) {
            println "GroovyServ Version: Server: @GROOVYSERV_VERSION@"
        }
    }

    private awaitAllSubThreads() {
        def threads = getAllAliveSubThreads()
        if (!threads) {
            LogUtils.debugLog "Threre is no sub thread"
            return
        }
        LogUtils.debugLog "All sub threads are joining..."
        for (Thread thread in threads) {
            if (thread.daemon) continue
            while (thread.alive) {
                if (interrupted) {
                    LogUtils.debugLog "Detected interruption while joining ${thread}"
                    return
                }
                LogUtils.debugLog "Joining ${thread}..."
                try {
                    thread.join(TIMEOUT_FOR_JOINING_SUBTHREADS)
                } catch (InterruptedException e) {
                    LogUtils.debugLog "Interrupted joining ${thread}"
                    throw e
                }
            }
        }
        LogUtils.debugLog "All sub threads joined"
    }

    private killAllSubThreadsIfExist() {
        def threads = getAllAliveSubThreads()
        if (!threads) {
            return
        }
        threads.each { thread ->
            thread.stop() // by force
        }
        LogUtils.debugLog "All sub threads stopped by force"
    }

    private Collection<Thread> getAllAliveSubThreads() {
        def threadGroup = Thread.currentThread().threadGroup
        Thread[] threads = new Thread[threadGroup.activeCount() + 1] // need at lease one extra space (see Javadoc of ThreadGroup)
        int count = threadGroup.enumerate(threads)
        if (count < threads.size()) {
            // convert to list for convenience except own thread
            def list = threads.toList().findAll { it && it != Thread.currentThread() }
            LogUtils.debugLog "Found ${list.size()} sub thread(s): ${list}"
            return list
        }
        return getAllAliveSubThreads()
    }
}

