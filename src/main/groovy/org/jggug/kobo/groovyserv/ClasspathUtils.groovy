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
class ClasspathUtils {

    static addClasspath(classpath) {
        def cp = System.getProperty("groovy.classpath")
        if (cp == null || cp == "") {
            System.setProperty("groovy.classpath", classpath)
        }
        else {
            def pathes = cp.split(File.pathSeparator) as List
            def pathToAdd = ""
            classpath.split(File.pathSeparator).reverseEach {
                if (!(pathes as List).contains(it)) {
                    pathToAdd = (it + File.pathSeparator + pathToAdd)
                }
            }
            System.setProperty("groovy.classpath", pathToAdd + cp)
        }
    }

}

