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
package groovyx.groovyserv.utils

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * @author NAKANO Yasuharu
 */
class IOUtils {

    /**
     * @throws Throwable when ExecutionException is occured, throw an inner exception wrapped by ExecutionException
     * @throws CancellationException
     * @throws InterruptedException
     */
    static awaitFuture(Future future) {
        try {
            future.get()
        } catch (ExecutionException e) {
            throw e.cause
        }
    }

    static close(closeable) {
        try {
            if (closeable) closeable.close()
        } catch (IOException e) {
            LogUtils.errorLog "Failed to close", e
        }
    }

    /**
     * @throws InterruptedIOException
     * @throws IOException
     */
    static readLines(InputStream ins) {
        def lines = []
        def line
        while ((line = readLine(ins)) != "") { // because in request, an empty line means the end of header part
            lines << line
        }
        return lines
    }

    /**
     * @throws InterruptedIOException
     * @throws IOException
     */
    static readLine(InputStream ins) {
        def baos = new ByteArrayOutputStream()
        int ch
        while ((ch = ins.read()) != -1) {
            if (ch == '\n') { // LF (fixed)
                break
            }
            baos.write((byte) ch)
        }
        return baos.toString()
    }

    /**
     * Reading and return available bytes.
     * This method is not blocking.
     *
     * @throws InterruptedIOException
     * @throws IOException
     */
    static readAvailableText(InputStream ins) {
        def byteList = new ArrayList<Byte>()
        int length
        while ((length = ins.available()) > 0) {
            byte[] bytes = new byte[length]
            int ret = ins.read(bytes, 0, length)
            if (ret == -1) {
                break
            }
            for (int i = 0; i < ret; i++) {
                byteList.add(bytes[i])
            }
        }
        return new String(byteList as byte[])
    }
}

