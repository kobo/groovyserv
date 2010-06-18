/*
 * Copyright 2009-2010 the original author or authors.
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
package org.jggug.kobo.groovyserv

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.ClasspathUtils} class.
 */
class ClasspathUtilsTest extends GroovyTestCase {

    private String originalClasspath

    void setUp() {
        originalClasspath = System.properties["groovy.classpath"]
        System.properties.remove("groovy.classpath")
    }

    void tearDown() {
        if (originalClasspath) {
            System.properties["groovy.classpath"] = originalClasspath
        } else {
            System.properties.remove("groovy.classpath")
        }
    }

    void testAddClasspath_null_to_one() {
        assert System.properties["groovy.classpath"] == null
        ClasspathUtils.addClasspath("path1")
        assert System.properties["groovy.classpath"] == "path1"
    }

    void testAddClasspath_null_to_multi() {
        assert System.properties["groovy.classpath"] == null
        def path = ['path1', 'path2', 'path3']
        ClasspathUtils.addClasspath(path.join(File.pathSeparator))
        assert System.properties["groovy.classpath"] == path.join(File.pathSeparator)
    }

    void testAddClasspath_one_to_multi() {
        System.properties["groovy.classpath"] = "path0"

        def path = ['path1', 'path2', 'path3']
        assert System.properties["groovy.classpath"] == "path0"
        ClasspathUtils.addClasspath(path.join(File.pathSeparator))
        def pathExpected = ['path0', 'path1', 'path2', 'path3']
        assert System.properties["groovy.classpath"] == pathExpected.join(File.pathSeparator)
    }

    void testAddClasspath_multi_to_multi() {
        def path1 = ['path0', 'pathA', 'pathB']
        System.properties["groovy.classpath"] = path1.join(File.pathSeparator)

        assert System.properties["groovy.classpath"] == path1.join(File.pathSeparator)

        def path2 = ['path1', 'path2', 'path3']
        ClasspathUtils.addClasspath(path2.join(File.pathSeparator))
        assert System.properties["groovy.classpath"] == (path1+path2).join(File.pathSeparator)
    }

    void testAddClasspath_duplicated() {
        def path1 = ['path0', 'path1', 'path2']
        System.properties["groovy.classpath"] = path1.join(File.pathSeparator)

        assert System.properties["groovy.classpath"] == path1.join(File.pathSeparator)
        def path2 = ['path3', 'path2', 'path1']
        ClasspathUtils.addClasspath(path2.join(File.pathSeparator))

        def pathExpected = ['path0', 'path1', 'path2', 'path3']
        assert System.properties["groovy.classpath"] == pathExpected.join(File.pathSeparator)
    }

    void testAddClasspath_emptyString_multi() {
        def path1 = ['path0', 'path1', 'path2']
        System.properties["groovy.classpath"] = path1.join(File.pathSeparator)

        assert System.properties["groovy.classpath"] == path1.join(File.pathSeparator)
        
        def path2 = ['', '', 'path3', '', 'path4', '', '', '']
        ClasspathUtils.addClasspath(path2.join(File.pathSeparator))

        def pathExpected = ['path0','path1','path2','path3','path4']
        assert System.properties["groovy.classpath"] == pathExpected.join(File.pathSeparator)
    }

    void testAddClasspath_emptyString_one() {
        System.properties["groovy.classpath"] = "path0:path1:path2"

        assert System.properties["groovy.classpath"] == "path0:path1:path2"
        ClasspathUtils.addClasspath("")
        assert System.properties["groovy.classpath"] == "path0:path1:path2"
    }

    void testAddClasspath_null_to_null() {
        assert System.properties["groovy.classpath"] == null
        ClasspathUtils.addClasspath(null)
        assert System.properties["groovy.classpath"] == null
    }

    void testAddClasspath_one_to_null() {
        System.properties["groovy.classpath"] = "path0"

        assert System.properties["groovy.classpath"] == "path0"
        ClasspathUtils.addClasspath(null)
        assert System.properties["groovy.classpath"] == "path0"
    }

}

