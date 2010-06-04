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

import static org.jggug.kobo.groovyserv.ClientConnection.*


/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class RequestWorker implements Runnable {

    private static currentDir // TODO extract into CurrentDirHolder

    private ClientConnection conn

    RequestWorker(clientConnection) {
        this.conn = clientConnection
    }

    @Override
    void run() {
        try {
            Map<String, List<String>> headers = conn.readHeaders()
            checkHeaders(headers)

            def cwd = headers[HEADER_CURRENT_WORKING_DIR][0]
            if (currentDir != null && currentDir != cwd) {
                throw new GroovyServerException(
                    "Can't change current directory because of another session running on different dir: " +
                    headers[HEADER_CURRENT_WORKING_DIR][0])
            }
            setCurrentDir(cwd)

            process(headers)
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
            setCurrentDir(null)
            conn.close()
        }
    }

    private checkHeaders(headers) {
        if (headers[HEADER_CURRENT_WORKING_DIR] == null || headers[HEADER_CURRENT_WORKING_DIR][0] == null) {
            throw new GroovyServerException("required header 'Cwd' is not specified.")
        }
        def givenCookie = headers[HEADER_COOKIE]?.getAt(0)
        if (!conn.cookie.isValid(givenCookie)) {
            Thread.sleep(5000)
            throw new GroovyServerException("authentication failed. cookie is unmatched: " + givenCookie)
        }
    }

    private process(headers) {
        if (headers[HEADER_CP] != null) {
            addClasspath(headers[HEADER_CP][0])
        }

        List args = headers[HEADER_ARG]
        for (Iterator<String> it = args.iterator(); it.hasNext(); ) {
            String s = it.next()
            if (s == "-cp") {
                it.remove()
                if (!it.hasNext()) {
                    throw new GroovyServerException("classpath option is invalid.")
                }
                String classpath = it.next()
                addClasspath(classpath)
                it.remove()
            }
        }
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

    private synchronized static setCurrentDir(newDir) {
        if (newDir == currentDir) return

        currentDir = newDir
        if (newDir) {
            System.setProperty('user.newDir', newDir)
            PlatformMethods.chdir(newDir)
            addClasspath(newDir)
        } else {
            //System.properties.remove('user.newDir')
            //PlatformMethods.chdir(currentDir)
            //removeClasspath(currentDir)
        }
    }

    private static addClasspath(newPath) { // FIXME this method is awful...
        if (newPath == null || newPath == "") {
            System.properties.remove("groovy.classpath")
            return
        }
        def pathes = newPath.split(File.pathSeparator) as LinkedHashSet
        pathes << newPath
        System.setProperty("groovy.classpath", pathes.join(File.pathSeparator))
    }

}

