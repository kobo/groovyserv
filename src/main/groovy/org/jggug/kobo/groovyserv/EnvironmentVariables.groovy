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
@Singleton
class EnvironmentVariables {

    private final cache = [:]
    private final origGetenv = System.metaClass.getMetaMethod("getenv", [String] as Object[])

    /**
     * Initialize something necessary.
     */
    static void setUp() {
        EnvironmentVariables.instance.replaceSystemGetenv()
    }

    /**
     * Replace System.getenv by the platform native method.
     */
    private void replaceSystemGetenv() {
        // adding the appropriate MOP method only for the current platform
        // to avovid overhead at each call of System.getenv()
        System.metaClass.static.getenv = createGetenv()
        DebugUtils.verboseLog("System.getenv is replaced")
    }

    /**
     * return the closure as MOP method for getting the environment variable.
     * The returned closure is used for replacement of System.getenv()
     * and then it's called by user script.
     */
    private Closure createGetenv() {
        // FIXME unified to using 1:cache and 2:System.getenv for both platforms
        if (Platform.isWindows()) {
            return { String envVarName ->
                String value = cache[envVarName]
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
     * set the environment variable by using a platform native method.
     * This method is called by groovyserver before invoking Groovy script.
     *
     * @param envVar 'NAME=VALUE' style environment variable information.
     */
    void put(String envVar) {
        // Parsing name and value
        def tokens = envVar.split('=', 2)
        def name = tokens[0]
        def value = (tokens.size() == 1) ? null : tokens[1]

        // Caching for next call of System.getenv
        cache[name] = value

        // Appling to native platform environment variables
        // for subprocess which is maybe invoked by user's Groovy script
        if (Platform.isWindows()) {
            CLibrary.INSTANCE._putenv(envVar)
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
}
