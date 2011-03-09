/*
 * Copyright 2009-2011 the original author or authors.
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

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform

/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class PlatformMethods {

    private static final LIBC
    static {
        if (Platform.isWindows()) {
            LIBC = Native.loadLibrary("msvcrt20", WindowsLibC.class)
        } else {
            LIBC = Native.loadLibrary("c", UnixLibC.class)
        }
    }

    /**
     * Change the current working directory to dir.
     *
     * @param dir directory to be set as current working directory.
     */
    static void chdir(String dir) {
        if (Platform.isWindows()) {
            LIBC._chdir(dir)
        } else {
            LIBC.chdir(dir)
        }
    }

    /**
     * Set the environment variable by using a platform native method.
     * This method is called by groovyserver before invoking Groovy script.
     *
     * @param envVar 'NAME=VALUE' style environment variable information.
     */
    static void putenv(String envVar) {
        if (Platform.isWindows()) {
            LIBC._putenv(envVar)
        } else {
            // You can't use putenv() here in UN*X. Because
            // putenv() keeps the reference to original string, so
            // when the JVM GC collects the area, envrironment loses
            // the var and getenv() can't find the var.
            def tokens = envVar.split('=', 2)
            def name = tokens[0]
            def value = (tokens.size() == 1) ? null : tokens[1]
            LIBC.setenv(name, value, 1)
        }
    }
}

