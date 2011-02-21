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

import com.sun.jna.Platform


/**
 * @author UEHARA Junji
 */
class PlatformMethods {

   /**
     * set the current working directory to dir.
     *
     * @param dir directory to be set as current working directory.
     */
    static chdir(String dir) {
        if (Platform.isWindows()) {
            CLibrary.INSTANCE._chdir(dir)
        }
        else {
            CLibrary.INSTANCE.chdir(dir)
        }
    }

    /**
     * set the current working directory to dir.
     *
     * @param envVar 'NAME=VALUE' style environmet variable information.
     */
    static putenv(String envVar) {
        def (name, value) = envVar.split('=', 2)
        if (Platform.isWindows()) {
            CLibrary.INSTANCE._putenv(envVar)
            EnvironmentVariables.putenvAsCache(name, value)
        }
        else {
            // You can't use putenv() in UNI*X here. Because
            // putenv() keeps the reference to original string, so
            // when the JVM GC correct the area, envrironment lose the var
            // and getenv() can't find that var.
            CLibrary.INSTANCE.setenv(name, value, 1)
        }
    }

    static String getenv(String envVarName) {
        return CLibrary.INSTANCE.getenv(envVarName)
    }

}

