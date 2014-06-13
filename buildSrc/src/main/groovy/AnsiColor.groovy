/*
 * Copyright 2009-2013 the original author or authors.
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

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

class AnsiColor {

    static init() {
        AnsiConsole.systemInstall()
        if (Boolean.valueOf(System.properties['no.color'] ?: false)) {
            Ansi.enabled = false
        }
    }

    static printlnAsInfo(text) {
        println Ansi.ansi().fg(Ansi.Color.CYAN).a(text).reset()
    }

    static printlnAsWarn(text) {
        println Ansi.ansi().fg(Ansi.Color.YELLOW).a(text).reset()
    }

    static printlnAsError(text) {
        println Ansi.ansi().fg(Ansi.Color.RED).a(text).reset()
    }

    static printlnAsTestResult(text, noLF = false) {
        def message = Ansi.ansi().render(text.replaceAll(/(SUCCESS)/, "@|green \$1|@").replaceAll(/(ERROR|FAILURE|FAILED)/, "@|red \$1|@"))
        if (noLF) {
            print message
            System.out.flush()
        } else {
            println message
        }
    }
}
