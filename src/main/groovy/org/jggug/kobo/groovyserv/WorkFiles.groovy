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

/**
 * @author NAKANO Yasuharu
 */
class WorkFiles {

    static final File DATA_DIR = new File("${System.getProperty('user.home')}/.groovy/groovyserv")
    static File LOG_FILE
    static File AUTHTOKEN_FILE

    static {
        setUp(GroovyServer.DEFAULT_PORT)
    }

    private static initWorkDir() {
        if (!DATA_DIR.isDirectory()) {
            DATA_DIR.mkdirs()
        }
    }

    static setUp(int port) {
        initWorkDir()
        LOG_FILE = new File(DATA_DIR, "groovyserver-${port}.log")
        AUTHTOKEN_FILE = new File(DATA_DIR, "authtoken-${port}")
    }
}

