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


/**
 * @author NAKANO Yasuharu
 */
@Singleton
class CurrentDirHolder {

    private static final String ORIGINAL_USER_DIR = System.properties["user.dir"]

    private currentDir

    synchronized void set(newDir) {
        if (!isChanged(newDir)) {
            return
        }
        if (isSetCurrentDir()) {
            throw new GroovyServerException("cannot change current directory because another session is running on different directory: ${newDir}")
        }
        System.setProperty('user.dir', newDir)
        PlatformMethods.chdir(newDir)
        ClasspathUtils.addClasspath(newDir)
        currentDir = newDir
    }

    synchronized void unset() {
        if (!isSetCurrentDir()) {
            return
        }
        System.setProperty('user.dir', ORIGINAL_USER_DIR)
        PlatformMethods.chdir(ORIGINAL_USER_DIR)
        ClasspathUtils.removeClasspath(currentDir)
        currentDir = null
    }

    private boolean isSetCurrentDir() {
        currentDir != null
    }

    private boolean isChanged(newDir) {
        currentDir == newDir
    }

}

