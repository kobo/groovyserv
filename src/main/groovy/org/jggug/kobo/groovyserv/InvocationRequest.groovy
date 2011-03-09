/*
 * Copyright 2009-2011 the original author or authors.
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
package org.jggug.kobo.groovyserv


/**
 * @author NAKANO Yasuharu
 */
class InvocationRequest {

    int port
    String cwd           // required: current working directory
    String classpath     // optional
    List<String> args    // required
    String clientCookie  // required
    Cookie serverCookie  // required
    List<String> envVars // optional

    InvocationRequest(map) {
        this.port = map.port
        this.cwd = map.cwd
        this.classpath = map.classpath
        this.args = map.args
        this.clientCookie = map.clientCookie
        this.serverCookie = map.serverCookie
        this.envVars = map.envVars
    }

    /**
     * @throws InvalidRequestHeaderException
     */
    void check() {
        if (!cwd) {
            throw new InvalidRequestHeaderException("'Cwd' header is not found: ${port}")
        }
        if (!clientCookie || !serverCookie.isValid(clientCookie)) {
            Thread.sleep(5000) // to prevent from brute force atack
            throw new InvalidRequestHeaderException("Authentication failed. Cookie is unmatched: ${clientCookie} <=> ${serverCookie.token}")
        }
    }

}
