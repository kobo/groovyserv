/*
 * Copyright 2009-2013 the original author or authors.
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

import org.jggug.kobo.groovyserv.exception.InvalidAuthTokenException
import org.jggug.kobo.groovyserv.exception.InvalidRequestHeaderException
import org.jggug.kobo.groovyserv.utils.DebugUtils

/**
 * @author NAKANO Yasuharu
 */
class InvocationRequest {

    int port
    String cwd                 // optional
    String classpath           // optional
    List<String> args          // optional
    String clientAuthToken     // required
    AuthToken serverAuthToken  // required
    List<String> envVars       // optional
    String protocol            // optional

    /**
     * @throws InvalidRequestHeaderException
     */
    void check() {
        if (!clientAuthToken || !serverAuthToken.isValid(clientAuthToken)) {
            Thread.sleep(5000) // to prevent from brute force attack
            DebugUtils.errorLog "Authentication failed. AuthToken is unmatched: ${clientAuthToken} <=> ${serverAuthToken.token}"
            throw new InvalidAuthTokenException("Authentication failed. AuthToken is unmatched: ${clientAuthToken} <=> ******")
        }
    }
}
