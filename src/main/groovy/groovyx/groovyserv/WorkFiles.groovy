/*
 * Copyright 2009 the original author or authors.
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
package groovyx.groovyserv

/**
 * @author NAKANO Yasuharu
 */
class WorkFiles {

    private static final File WORK_DIR = new File(System.getenv("GROOVYSERV_WORK_DIR") ?: "${System.getProperty('user.home')}/.groovy/groovyserv")
    private static final LOG_DIR = new File(System.getenv("GROOVYSERV_LOG_DIR") ?: WORK_DIR.absolutePath)
    static File LOG_FILE
    static File AUTHTOKEN_FILE

    static {
        setUp(GroovyServer.DEFAULT_PORT)
    }

    static setUp(int port) {
        makeSureDirs()
        LOG_FILE = new File(LOG_DIR, "groovyserver-${port}.log")
        AUTHTOKEN_FILE = new File(WORK_DIR, "authtoken-${port}")
    }

    private static makeSureDirs() {
        if (!WORK_DIR.isDirectory()) {
            WORK_DIR.mkdirs()
        }
        if (!LOG_DIR.isDirectory()) {
            LOG_DIR.mkdirs()
        }
    }
}

