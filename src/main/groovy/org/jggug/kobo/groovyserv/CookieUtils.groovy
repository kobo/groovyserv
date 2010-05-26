#!/usr/bin/env groovy
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

// TODO refactoring
class CookieUtils {

    private CookieUtils() { /* forbidden instantiation */ }

    static String createCookie() {
        File file1 = new File(System.getProperty('user.home')+'/.groovy')
        boolean b = file1.exists()
        if (!file1.exists()) {
            if (file1.mkdir() == false) {
                throw new GroovyServerException("failed to create directory:"+file1.path)
            }
        }

        File file2 = new File(file1.path + '/groovyserver')
        if (!file2.exists()) {
            if (file2.mkdir() == false) {
                throw new GroovyServerException("failed to create directory:"+file2.path)
            }
        }

        File file3 = new File(file2.path + "/key")
        if (file3.exists()) {
            if (file3.delete() == false) {
                throw new GroovyServerException("failed to delete file:"+file3.path)
            }
        }
        if (file3.createNewFile() == false) {
            throw new GroovyServerException("failed to create file:"+file3.path)
        }
        file3.setReadable(false, false)
        file3.setReadable(true, true)

        String result = Long.toHexString(new Random().nextLong())
        try {
            FileOutputStream dos = new FileOutputStream(file3)
            dos.write(result.getBytes())
            dos.close()
        }
        catch (IOException e) {
            throw new GroovyServerException("can't write to key file:"+file3.path)
        }

        file3.setReadable(false, false)
        file3.setReadable(true, true)

        return result
    }

}

