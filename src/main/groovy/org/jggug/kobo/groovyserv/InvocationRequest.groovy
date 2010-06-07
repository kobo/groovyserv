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

import static org.jggug.kobo.groovyserv.ClientConnection.*


/**
 * @author NAKANO Yasuharu
 */
class InvocationRequest {

    private ClientConnection conn

    String cwd           // required: current working directory
    String classpath     // optional
    String cookie        // required
    List<String> args    // required

    InvocationRequest(ClientConnection conn) {
        this.conn = conn
        readAndSetUp()
    }

    private void readAndSetUp() {
        Map<String, List<String>> headers = conn.readHeaders()
        this.cwd = headers[HEADER_CURRENT_WORKING_DIR][0]
        this.cookie = headers[HEADER_COOKIE]?.getAt(0)
        this.classpath = headers[HEADER_CP]?.getAt(0)
        this.args = headers[HEADER_ARG]
        checkHeaders(headers)
    }

    private checkHeaders(headers) {
        if (!cwd) {
            throw new GroovyServerException("required header 'Cwd' is not specified.")
        }
        if (!cookie || !conn.cookie.isValid(cookie)) {
            Thread.sleep(5000) // to prevent from brute force atack
            throw new GroovyServerException("authentication failed. cookie is unmatched: " + cookie)
        }
        if (!args) {
            throw new GroovyServerException("required header 'Args' is not specified.")
        }
    }

}
