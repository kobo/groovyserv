/*
 * Copyright 2014 the original author or authors.
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

// The base title of page
title = "GroovyServ"

// The version of this documantation depends on the product version
version = System.getenv("GROOVYSERV_VERSION") ?: "X.X.X-SNAPSHOT"

// The path of source files directory
sourceDirectoryPath = "source"

// The path of directory to be outputted a document
outputDirectoryPath = "../build/docs"

// Turn off numbering for topics
numbering = false

// Build information
groovyVersion = System.getenv("GROOVY_VERSION") ?: "X.X.X"
javaVersion = "1.8.0_11"

// Filters
filters = {
    fontAwesome {
        before = { text ->
            text.replaceAll(/(?m)^( *[*-]) (.+) \{\.fa\.fa-([a-z-]+)\}$/, '$1 <i class="fa fa-$3"></i>$2')
        }
    }
    expandVersion {
        before = { text ->
            text.
                replaceAll(/(?m)<VERSION>/, version).
                replaceAll(/(?m)<GROOVY_VERSION>/, groovyVersion).
                replaceAll(/(?m)<JAVA_VERSION>/, javaVersion)
        }
    }
}
