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
package org.jggug.kobo.groovyserv

import static org.junit.Assert.*

/**
 * Utilities only for tests.
 */
class TestUtils {

    static executeClientOk(args, closure = null) {
        def p = executeClient(args, closure)
        if (p.exitValue() != 0) {
            fail "ERROR: exitValue:${p.exitValue()}, in:[${p.in.text}], err:[${p.err.text}]"
        }
        return p
    }

    static executeClient(args, closure = null) {
        def p = createProcessBuilder(args).start()
        if (closure) closure.call(p)
        p.waitFor()
        return p
    }

    static executeClientOkWithEnv(args, Map envMap, closure = null) {
        def p = executeClientWithEnv(args, envMap, closure)
        if (p.exitValue() != 0) {
            fail "ERROR: exitValue:${p.exitValue()}, in:[${p.in.text}], err:[${p.err.text}]"
        }
        return p
    }

    static executeClientWithEnv(args, Map envMap, closure = null) {
        def p = createProcessBuilder(args, envMap).start()
        if (closure) closure.call(p)
        p.waitFor()
        return p
    }

    static createProcessBuilder(args, Map envMap = [:]) {
        def clientExecs = System.properties.'groovyservClientExecutable'.split(" ") as List
        ProcessBuilder processBuilder = new ProcessBuilder()
        def command = processBuilder.command()

        // This doesn't work on cygwin/windows. command line is somehow split by white space.
//        def env = processBuilder.environment()
//        envMap.each { key, value ->
//            env.put(key.toString(), value.toString()) // without this, ArrayStoreException may occur
//        }
        command << "env"
        envMap.each { key, value ->
            command << "${key}=${value}".toString() // without this, ArrayStoreException may occur
        }

        [* clientExecs, * args].each { arg ->
            command << arg.toString() // without this, ArrayStoreException may occur
        }

        return processBuilder
    }

    /**
     * Reading and return available bytes.
     * This method is not blocking.
     */
    static getAvailableText(ins) {
        def byteList = new ArrayList<Byte>()
        int length
        while ((length = ins.available()) > 0) {
            byte[] bytes = new byte[length]
            int ret = ins.read(bytes, 0, length)
            if (ret == -1) {
                break
            }
            for (int i = 0; i < ret; i++) {
                byteList.add(bytes[i])
            }
        }
        return new String(byteList as byte[])
    }

}
