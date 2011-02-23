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
 * @author NAKANO Yasuharu
 */
class PlatformMethods {

    private static final envCacheForWin = [:]
    private static final origGetenv = System.metaClass.getMetaMethod("getenv", [String] as Object[])

    /**
     * change the current working directory to dir.
     *
     * @param dir directory to be set as current working directory.
     */
    static void chdir(String dir) {
        if (Platform.isWindows()) {
            CLibrary.INSTANCE._chdir(dir)
        }
        else {
            CLibrary.INSTANCE.chdir(dir)
        }
    }

    /**
     * set the environment variable by using a platform native method.
     * This method is called by groovyserver before invoking Groovy script.
     *
     * @param envVar 'NAME=VALUE' style environment variable information.
     */
    static void putenv(String envVar) {
        def tokens = envVar.split('=', 2)
        def name = tokens[0]
        def value = (tokens.size() == 1) ? null : tokens[1]
        if (Platform.isWindows()) {
            CLibrary.INSTANCE._putenv(envVar)
            envCacheForWin[name] = value
        }
        else {
            // You can't use putenv() here in UN*X. Because
            // putenv() keeps the reference to original string, so
            // when the JVM GC collects the area, envrironment loses
            // the var and getenv() can't find the var.
            CLibrary.INSTANCE.setenv(name, value, 1)
        }
        DebugUtils.verboseLog("putenv(${name}, ${value})")
    }

    /**
     * return the closure as MOP method for getting the environment variable.
     * The returned closure is used for replacement of System.getenv()
     * and then it's called by user script.
     *
     * @param envVarName name of the environment variable
     */
    private static Closure createGetenv() {
        if (Platform.isWindows()) {
            return { String envVarName ->
                // FIXME Why does cache need only for Windows?
                String value = envCacheForWin[envVarName]
                if (value == null) {
                    value = origGetenv.doMethodInvoke(System, name)
                }
                DebugUtils.verboseLog("getenv(${envVarName}) => $value")
                return value
            }
        }
        else {
            return { String envVarName ->
                String value = CLibrary.INSTANCE.getenv(envVarName)
                DebugUtils.verboseLog("getenv(${envVarName}) => $value")
                return value
            }
        }
    }

    /**
     * Replace System.getenv by the platform native method.
     */
    static void replaceSystemGetenv() {
        // adding the appropriate MOP method only for the current platform
        // to avovid overhead at each call of System.getenv()
        System.metaClass.static.getenv = createGetenv()
        DebugUtils.verboseLog("System.getenv is replaced")
    }

}

