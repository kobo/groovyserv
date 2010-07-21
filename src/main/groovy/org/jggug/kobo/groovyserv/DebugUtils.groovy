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

    static isVerboseMode() {
        Boolean.valueOf(System.getProperty("groovyserver.verbose"))
    }

    static errorLog(message, Throwable e = null) {
        def formatted = formatLog(message, e)
        writeLog(formatted)
    }

    static infoLog(message, Throwable e = null) {
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
        def timestamp = currentTimestamp() // use same timestamp per call of formatLog
        message.eachLine { line ->
            pw.println(timestamp + " " + line)
        }
        if (e) {
            ("---> " + formatStackTrace(e)).eachLine { line ->
                pw.println(timestamp + " " + line)
            }
        }
        sw.toString().trim()
    }

    private static currentTimestamp() {
        new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date())
    }

    private static writeLog(formatted) {
        FileUtils.LOG_FILE.withWriterAppend { out ->
            out.println formatted
        }
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

    static String dump(byte[] buf, int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("offset and length must be specified a positive value")
        }
        final separatorLine = "+-----------+-----------+-----------+-----------+----------------+"
        final numPerLine = 16
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        pw.println(separatorLine)
        int maxIndex = [buf.size(), offset + length].min()
        for (int startIndex = offset; startIndex < maxIndex; startIndex += numPerLine) { // for each 16 elements
            int endIndex = [startIndex + numPerLine, maxIndex].min()
            def elementsAtLine = buf[startIndex..<endIndex]
            def completed = (0..15).collect{ elementsAtLine[it] ?: null }
            def digitPart = completed.collect{ toDisplayDigit(it) }.join(" ")
            def asciiPart = completed.collect{ toDisplayAscii(it) }.join()
            pw.println(digitPart + " | " + asciiPart)
        }
        pw.print(separatorLine)
        sw.toString()
    }

    private static toDisplayDigit(b) {
        if (b) {
            return String.format("%02x", b)
        }
        return "  "
    }

    private static toDisplayAscii(b) {
        if (b) {
            char c = (char) b
            return (c.isLetterOrDigit() ? c : "?")
        }
        return ""
    }
 
}

