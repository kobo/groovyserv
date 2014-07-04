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
package groovyx.groovyserv.platform

import groovyx.groovyserv.exception.GServIllegalStateException

/**
 * @author NAKANO Yasuharu
 */
@Singleton
class CurrentDirHolder {

    private static final String ORIGINAL_USER_DIR = System.properties["user.dir"]
    private static final String ORIGINAL_PWD = System.getenv("PWD")

    private volatile currentDir

    /**
     * @throws GServIllegalStateException
     *              When changed current directory after set different directory by another session
     */
    synchronized void setDir(newDir) {
        if (!isChanged(newDir)) {
            return
        }
        if (isSetCurrentDir()) {
            throw new GServIllegalStateException("Cannot change current directory because another session is running on different directory: ${currentDir} -X-> ${newDir}")
        }
        System.properties['user.dir'] = newDir
        PlatformMethods.chdir(newDir)
        EnvironmentVariables.instance.put("PWD=$newDir")
        currentDir = newDir
    }

    synchronized void reset() {
        if (!isSetCurrentDir()) {
            return
        }
        System.properties['user.dir'] = ORIGINAL_USER_DIR
        EnvironmentVariables.instance.put("PWD=$ORIGINAL_PWD")
        PlatformMethods.chdir(ORIGINAL_USER_DIR)
        currentDir = null
    }

    private synchronized boolean isSetCurrentDir() {
        currentDir != null
    }

    private synchronized boolean isChanged(newDir) {
        currentDir != newDir
    }

}

