/*
 * Copyright 2009-2011 the original author or authors.
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
package org.jggug.kobo.groovyserv.stream

/**
 * Dynamically delegatable InputStream.
 *
 * @author NAKANO Yasuharu
 */
class DynamicDelegatedInputStream extends InputStream {

    private Closure delegate

    DynamicDelegatedInputStream(Closure delegate) {
        this.delegate = delegate
    }

    private InputStream getInputStream() {
        delegate.call()
    }

    @Override
    int read() {
        getInputStream().read()
    }

    @Override
    int read(byte[] buf) {
        getInputStream().read(buf)
    }

    @Override
    int read(byte[] buf, int offset, int length) {
        getInputStream().read(buf, offset, length)
    }

    @Override
    void close() {
        getInputStream().close()
    }

    @Override
    int available() {
        getInputStream().available()
    }

    @Override
    void mark(int readlimit) {
        getInputStream().mark()
    }

    @Override
    void reset() {
        getInputStream().reset()
    }

    @Override
    long skip(long n) {
        getInputStream().skip(n)
    }

    @Override
    boolean markSupported() {
        getInputStream().markSupported()
    }

}
