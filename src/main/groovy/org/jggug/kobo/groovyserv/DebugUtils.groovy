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

    static errLog(message, Throwable e = null) {
        StreamManager.ORIGINAL.err.println message
        if (e) {
            StreamManager.ORIGINAL.err.println stackTrace(e)
        }
    }

    static debugLog(message) { // for DEBUG
        new File("/tmp/gs-log.txt").withWriterAppend { w -> w << message << '\n' }
    }

    static verboseLog(message) {
        if (isVerboseMode()) {
            StreamManager.ORIGINAL.err.println message
        }
    }

    static isVerboseMode() {
        System.getProperty("groovyserver.verbose") == "true" 
    }

    static String stackTrace(e) {
        StringWriter sw = new StringWriter()
        e.printStacktrace(sw)
        sw.toString()
    }

    static String dump(byte[] buf, int offset, int length) { // TODO refactoring
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        pw.println("+-----------+-----------+-----------+-----------+")

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
        sw.toString()
    }

}

