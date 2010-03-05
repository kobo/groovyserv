/*
 * Copyright 2009-2010 the original author or authors.
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

import groovy.util.GroovyTestCase

class TestUtils {

    static execute(command, closure = null) {
        Process p = command.execute()
        if (closure) closure.call(p)
        p.waitFor()
        //println "$command => ${p.exitValue()}"
        if (p.exitValue() != 0) {
            fail "ERROR: in:[${p.in.text}], err:[${p.err.text}]"
        }
        return p
    }

    static executeFail(command) {
        Process p = command.execute()
        p.waitFor()
        return p
    }

}
