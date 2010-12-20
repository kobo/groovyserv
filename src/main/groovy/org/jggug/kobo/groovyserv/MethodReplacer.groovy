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
 * Replace JDK methods by MOP.
 * @author UEHARA Junji
 */
class MethodReplacer {

    static def envVars = [:]

    public static void putenv(String name, String value) {
        DebugUtils.verboseLog("cache putenv(${name})to $value")
        envVars[name] = value
    }

    static origGetenv = System.metaClass.getMetaMethod("getenv", [String] as Object[])

    public static void replace() {
        DebugUtils.verboseLog("MethodReplacer.replace()")

        if (Platform.isWindows()) {
            System.metaClass.static.getenv = {String name ->
                                              String result = envVars[name]
                                              if (result == null) {
                                                  result = origGetenv.doMethodInvoke(delegate, name)
                                              }
                                              DebugUtils.verboseLog("getenv(${name})=>$result")
                                              return result;
            }
            
        }
        

        if (!Platform.isWindows()) {
            System.metaClass.static.getenv = {String name ->
                                              // Replace System.getenv() to platform native getenv(1) to skip cache.
                                              String result = PlatformMethods.getenv(name)
                                              DebugUtils.verboseLog("getenv(${name})=>$result")
                                              return result
            }
        }
    }

}
