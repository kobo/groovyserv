/*
 * Copyright 2009 the original author or authors.
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
package groovyx.groovyserv.utils

import groovyx.groovyserv.WorkFiles

/**
 * @author NAKANO Yasuharu
 */
class LogUtils {

    // MEMO: GroovyServ cannot use a major log library like Log4j because it may be used by a user script.
    // If GroovyServ configured log4j, its behavior on the user script could became something unexpected for user.

    static boolean debug = false

    static errorLog(message, Throwable e = null) {
        writeLog(formatLog("ERROR", message, e))
    }

    static infoLog(message, Throwable e = null) {
        writeLog(formatLog("INFO", message, e))
    }

    static debugLog(message, Throwable e = null) {
        if (!debug) return
        writeLog(formatLog("DEBUG", message, e))
    }

    private static String formatLog(String level, Object message, Throwable e) {
        def caller = callerInfo
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        def timestamp = currentTimestamp() // use same timestamp per call of formatLog
        String messageText = (message instanceof Closure) ? message.call() : message
        messageText.eachLine { line ->
            pw.println "${timestamp} [${level}] (${Thread.currentThread()?.threadGroup}) ($caller) ${line}"
        }
        if (e) {
            pw.println formatStackTrace(e)
        }
        sw.toString().trim()
    }

    private static getCallerInfo() {
        def caller = Thread.currentThread().stackTrace.find { StackTraceElement ele ->
            def className = ele.className
            className.startsWith("groovyx.groovyserv") && !className.startsWith(LogUtils.name)
        }
        def simpleName = caller.className.replaceFirst(/^.*\./, '') // as simpleName
        def lineNumber = caller.lineNumber
        return "${simpleName}:${lineNumber}"
    }

    private static currentTimestamp() {
        new Date().format("yyyy/MM/dd HH:mm:ss,SSS")
    }

    private static writeLog(String formatted) {
        WorkFiles.LOG_FILE.withWriterAppend { out ->
            out.println formatted
        }
    }

    private static String formatStackTrace(Throwable e) {
        def sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        sanitizeStackTrace(sw.toString())
    }

    private static String sanitizeStackTrace(String stackTrace) {
        def sw = new StringWriter()
        stackTrace.eachLine { line ->
            if (line=~/at (sun\.|org.codehaus.groovy)/) return
            sw.println line
        }
        sw.print("\t(sanitized)")
        sw.toString()
    }

    static String dumpHex(byte[] buf, int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("Must be positive: offset=${offset}, length=${length}")
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
            def completed = (0..15).collect { elementsAtLine[it] ?: null }
            def digitPart = completed.collect { toDisplayDigit(it) }.join(" ")
            def asciiPart = completed.collect { toDisplayAscii(it) }.join("")
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
            return (c==~/\p{Print}/) ? c : "?"
        }
        return ""
    }

}

