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
import org.jggug.kobo.groovyserv.exception.GServIOException
/**
 * A connection between client process and server process in localhost
 * is authenticated by simple authToken mechanism.
 *
 * @author NAKANO Yasuharu
 */
class AuthToken {

    final String token

    AuthToken(token = null) {
        this.token = token ?: createNewAuthToken()
    }

    private static createNewAuthToken() {
        Long.toHexString(new Random().nextLong())
    }

    void save() {
        try {
            WorkFiles.AUTHTOKEN_FILE.text = token
            setupFilePermission()
            //DebugUtils.verboseLog "Saved authToken: ${token}"
        } catch (IOException e) {
            throw new GServIOException("I/O error: AuthToken file cannot be written: ${WorkFiles.AUTHTOKEN_FILE}", e)
        }
    }

    void delete() {
        WorkFiles.AUTHTOKEN_FILE.delete()
        //DebugUtils.verboseLog "Deleted authToken: ${token}"
    }

    boolean isValid(given) {
        token == given
    }

    private static setupFilePermission() {
        // as 600 permission
        WorkFiles.AUTHTOKEN_FILE.with {
            setReadable(false, false)
            setWritable(false, false)
            setExecutable(false, false)
            setReadable(true, true)
            setWritable(true, true)
        }
    }
}

