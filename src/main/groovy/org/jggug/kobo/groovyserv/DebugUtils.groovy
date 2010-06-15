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
class DebugUtils {

    private static final String PREFIX_DEBUG_LOG = "DEBUG: "

    static errorLog(message, Throwable e = null) {
        def formatted = formatLog(message, e)
        writeLog(formatted)
    }

    static verboseLog(message, Throwable e = null) {
        if (!isVerboseMode()) return

        // added prefix for each line
        def formatted = {
            def sw = new StringWriter()
            def pw = new PrintWriter(sw)
            formatLog(message, e).eachLine { line ->
                pw.println(PREFIX_DEBUG_LOG + line)
            }
            sw.toString().trim()
        }.call()

        writeLog(formatted)
    }

    private static formatLog(message, Throwable e) {
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        pw.println message
        if (e) {
            pw << "---> " << formatStackTrace(e)
        }
        sw.toString().trim()
    }

    private static writeLog(formatted) {
        FileUtils.LOG_FILE.withWriterAppend { out ->
            out.println formatted
        }
    }

    static isVerboseMode() {
        System.getProperty("groovyserver.verbose") == "true"  // TODO Boolean.valueOf
    }

    private static String formatStackTrace(e) {
        def sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        sanitizeStackTrace(sw.toString())
    }

    private static String sanitizeStackTrace(string) {
       def sw = new StringWriter()
       string.eachLine { line ->
            if (line =~ /at (sun\.|org.codehaus.groovy)/) return
            sw.println line
        }
        sw.print("\t(sanitized)")
        sw.toString()
    }

    static String dump(byte[] buf, int offset, int length) { // TODO refactoring
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        pw.println("+-----------+-----------+-----------+-----------+----------------+")
        StringBuilder buff = new StringBuilder()
        int i
        for (i = offset; i < offset + length; i++) {
            if (!Character.isISOControl((char) buf[i])) {
                buff.append((char) buf[i])
            }
            else {
                buff.append("?")
            }
            pw.print(String.format("%02x ", buf[i]))
            if ((i - offset) % 16 == 15) {
                pw.print("| " + buff)
                pw.println()
                buff.length = 0
            }
        }
        if ((length % 16) != 0) {
            while (i < (length+16).intdiv(16)*16 + offset) {
                i++
                pw.print("   ")
            }
            pw.println("| " + buff)
        }
        pw.print("+-----------+-----------+-----------+-----------+----------------+")
        sw.toString()
    }

}

