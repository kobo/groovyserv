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

// The title of TOC
//tocTitle = "Table of contents"

// The path of source files directory
sourceDirectoryPath = "source"

// The encoding of input Markdown resource
//inputEncoding = "UTF-8"

// The encoding of output document
//outputEncoding = "UTF-8"

// The path of pages file path
pagesFilePath = "source/pages.groovy"

numbering = false

theme = "groovyserv"

version = "1.0-SNAPSHOT"

// Filters
filters = {
    admonition {
        before = { text ->
            text.replaceAll(/(?m)^> ###### (INFO|WARN)(?:: *(.+))?\n((?:.|\n)+?)^$/) { all, type, title, lines ->
                def header = title ? "###### $title" : ""
                """<div class="admonition admonition-${type.toLowerCase()}" markdown="1">
                  |$header
                  |${lines.replaceAll(/(?m)^> ?/, '')}
                  |</div>
                  |""".stripMargin()
            }
        }
    }
    fontAwesome {
        before = { text ->
            text.replaceAll(/(?m)^( *[*-]) (.+) \{\.fa\.fa-([a-z-]+)\}$/, '$1 <i class="fa fa-$3"></i>$2')
        }
    }
    expandVersion {
        before = { text ->
            text.replaceAll(/(?m)<VERSION>/, version)
        }
    }
}
