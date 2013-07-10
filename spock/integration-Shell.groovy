/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.jggug.kobo.groovyserv.DirectAccessSpec
import org.jggug.kobo.groovyserv.ExecScriptSpec
import org.jggug.kobo.groovyserv.ServerOperationFromClientSpec
import org.jggug.kobo.groovyserv.SystemExitSpec
import org.jggug.kobo.groovyserv.test.IgnoreForShellClient
import org.jggug.kobo.groovyserv.test.IntegrationTest

runner {
    include IntegrationTest

    def excludesList = [DirectAccessSpec, ExecScriptSpec, SystemExitSpec, IgnoreForShellClient]
    if (isWindows()) {
        excludesList << ServerOperationFromClientSpec
    }
    exclude(*excludesList)
}

private static String getOsName() {
    System.properties['os.name'].replaceAll(' ', '').toLowerCase()
}

private static boolean isWindows() {
    getOsName().startsWith("windows")
}