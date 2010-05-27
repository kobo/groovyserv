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

import java.util.concurrent.atomic.AtomicReference

import static org.jggug.kobo.groovyserv.ProtocolHeader.*


/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class RequestHandler implements Runnable {

    private final static CURRENT_DIR = new AtomicReference()

    Socket socket
    String cookie

    synchronized setCurrentDir(dir) {
        if (dir != CURRENT_DIR.get()) {
            CURRENT_DIR.set(dir)
            System.setProperty('user.dir', CURRENT_DIR.get())
            PlatformMethods.chdir(CURRENT_DIR.get())
            addClasspath(CURRENT_DIR.get())
        }
    }

    def addClasspath(classpath) {
        def cp = System.getProperty("groovy.classpath")
        if (cp == null || cp == "") {
            System.setProperty("groovy.classpath", classpath)
        }
        else {
            def pathes = cp.split(File.pathSeparator) as List
            def pathToAdd = ""
            classpath.split(File.pathSeparator).reverseEach {
                if (!(pathes as List).contains(it)) {
                    pathToAdd = (it + File.pathSeparator + pathToAdd)
                }
            }
            System.setProperty("groovy.classpath", pathToAdd + cp)
        }
    }

    def process(headers) {
        if (headers[HEADER_CP] != null) {
            addClasspath(headers[HEADER_CP][0])
        }

        List args = headers[HEADER_ARG]
        for (Iterator<String> it = headers[HEADER_ARG].iterator(); it.hasNext(); ) {
            String s = it.next()
            if (s == "-cp") {
                it.remove()
                String classpath = it.next()
                addClasspath(classpath)
                it.remove()
            }
        }
        GroovyMain2.main(args as String[])
    }

    def checkHeaders(headers) {
        if (headers[HEADER_CURRENT_WORKING_DIR] == null || headers[HEADER_CURRENT_WORKING_DIR][0] == null) {
            throw new GroovyServerException("required header cwd unspecified.")
        }
        if (cookie == null || headers[HEADER_COOKIE] == null || headers[HEADER_COOKIE][0] != cookie) {
            Thread.sleep(5000)
            throw new GroovyServerException("authentication failed.")
        }
    }

    def sendExit(outs, status) {
        outs.write((HEADER_STATUS+": "+status+"\n").bytes)
        outs.write("\n".bytes)
    }

    def ensureAllThreadToStop() {
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

    @Override
    void run() {
        try {
            socket.withStreams { ins, outs ->
                try {
                    Map<String, List<String>> headers = ProtocolHeader.readHeaders(ins)
                    if (DebugUtils.isVerboseMode()) {
                        headers.each {k,v ->
                            DebugUtils.errLog " $k = $v"
                        }
                    }
                    checkHeaders(headers)

                    def cwd = headers[HEADER_CURRENT_WORKING_DIR][0]
                    if (CURRENT_DIR.get() != null && CURRENT_DIR.get() != cwd) {
                        throw new GroovyServerException(
                            "Can't change current directory because of another session running on different dir: " +
                            headers[HEADER_CURRENT_WORKING_DIR][0])
                    }
                    setCurrentDir(cwd)

                    process(headers)
                    ensureAllThreadToStop()
                    sendExit(outs, 0)
                }
                catch (ExitException e) {
                    // GroovyMain2 throws ExitException when it catches ExitException.
                    sendExit(outs, e.exitStatus)
                }
                catch (Throwable e) {
                    DebugUtils.errLog("unexpected error", e)
                    sendExit(outs, 0)
                }
            }
        }
        finally {
            setCurrentDir(null)

            if (socket) socket.close()
            DebugUtils.verboseLog "socket is closed: $socket"
            socket = null
        }
    }

}

