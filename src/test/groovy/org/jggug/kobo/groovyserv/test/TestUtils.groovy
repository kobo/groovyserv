/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jggug.kobo.groovyserv.test

import org.jggug.kobo.groovyserv.GroovyClient

import static org.junit.Assert.*
/**
 * Utilities only for tests.
 */
class TestUtils {

    static Process executeClientScriptWithEnv(args, Map envMap, closure = null) {
        def client = clientExecutablePath.split(" ") as List
        def p = createProcessBuilder([* client, * args], envMap).start()
        if (closure) closure.call(p)
        p.waitFor()
        return p
    }

    static Process executeClientScript(args, closure = null) {
        executeClientScriptWithEnv(args, null, closure)
    }

    static Process executeClientScriptOkWithEnv(args, Map envMap, closure = null) {
        def p = executeClientScriptWithEnv(args, envMap, closure)
        if (p.exitValue() != 0) {
            fail "ERROR: exitValue:${p.exitValue()}, in:[${p.in.text}], err:[${p.err.text}]"
        }
        return p
    }

    static Process executeClientScriptOk(args, closure = null) {
        executeClientScriptOkWithEnv(args, null, closure)
    }

    static void startServerIfNotRunning() {
        if (new GroovyClient().isServerAvailable()) return

        def p = executeServerScript(["-r", "-v"])
        if (p.exitValue() != 0) {
            fail "ERROR: exitValue:${p.exitValue()}, in:[${p.in.text}], err:[${p.err.text}]"
        }
    }

    static void shutdownServerIfRunning() {
        if (new GroovyClient().isServerShutdown()) return

        def p = executeServerScript(["-k"])
        if (p.exitValue() != 0) {
            fail "ERROR: exitValue:${p.exitValue()}, in:[${p.in.text}], err:[${p.err.text}]"
        }
    }

    static Process executeServerScript(List options) {
        def p = createProcessBuilder(["sh", serverExecutablePath] + options).start()
        p.waitFor()
        return p
    }

    private static createProcessBuilder(List commandLine, Map envMap = [:]) {
        ProcessBuilder processBuilder = new ProcessBuilder()
        def actualCommand = processBuilder.command()

        // This doesn't work on cygwin/windows. command line is somehow split by white space.
//        def env = processBuilder.environment()
//        envMap.each { key, value ->
//            env.put(key.toString(), value.toString()) // without this, ArrayStoreException may occur
//        }
        actualCommand << "env"
        envMap.each { key, value ->
            actualCommand << "${key}=${value}".toString() // without this, ArrayStoreException may occur
        }

        // unset GROOVYSERV_HOME (which may be set by GVM) for a process invoked by a test
        processBuilder.environment().remove('GROOVYSERV_HOME')

        commandLine.each { arg ->
            actualCommand << arg.toString() // without this, ArrayStoreException may occur
        }

        return processBuilder
    }

    private static getClientExecutablePath() {
        System.getProperty("groovyserv.executable.client")
    }

    private static getServerExecutablePath() {
        System.getProperty("groovyserv.executable.server")
    }
}
