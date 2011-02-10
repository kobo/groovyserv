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
 * Utilities for handling environment variables.
 *
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class MethodReplacer { // FIXME rename to EnvUtils

    private static final envVars = [:]
    private static final origGetenv = System.metaClass.getMetaMethod("getenv", [String] as Object[])

    static void putenv(String name, String value) {
        DebugUtils.verboseLog("cache putenv(${name}, ${value})")
        envVars[name] = value
    }

    static void replace() { // FIXME rename to replaceMethodOfGetenv
        if (Platform.isWindows()) {
            System.metaClass.static.getenv = { String name ->
                String value = envVars[name]
                if (value == null) {
                    origGetenv.doMethodInvoke(delegate, name)
                }
                DebugUtils.verboseLog("getenv(${name}) => $value")
                return value
            }
        }
        else {
            System.metaClass.static.getenv = { String name ->
                // Replace System.getenv() to platform native getenv(1) to skip cache.
                String value = PlatformMethods.getenv(name)
                DebugUtils.verboseLog("getenv(${name}) => $value")
                return value
            }
        }
        DebugUtils.verboseLog("System.getenv is replaced")
    }

}
