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

filters = {
    admonition {
        before = { text ->
            def converter = { all, type, title, lines ->
                def header = title ? """<div class="admonition-caption">$title</div>""" : ""
                return """<div class="admonition admonition-${type.toLowerCase()}">
                         |  <div class="admonition-icon">
                         |    <i class="fa"></i>
                         |  </div>
                         |  <div class="admonition-content" markdown="1">
                         |    $header
                         |    ${lines.replaceAll(/(?m)^\s*>\s*?/, '')}
                         |  </div>
                         |</div>""".stripMargin()
            }
            text.
                replaceAll(/(?ms)^> \*\*(NOTE|TIP|WARNING|IMPORTANT)\*\*(?::(.*?))?$(.*?)(?:^$|> ----)/, converter). // block
                replaceAll(/(?ms)^\*\*(NOTE|TIP|WARNING|IMPORTANT)\*\*:()(.*?)$/, converter) // inline
        }
    }
}
