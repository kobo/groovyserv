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
package groovyx.groovyserv.stream

/**
 * Dynamically delegatable PrintStream.
 *
 * @author NAKANO Yasuharu
 */
class DynamicDelegatedPrintStream extends PrintStream {

    private Closure delegate

    DynamicDelegatedPrintStream(Closure delegate) {
        super(new ByteArrayOutputStream()) // dummy for instantiation
        this.delegate = delegate
    }

    private PrintStream getPrintStream() {
        delegate.call()
    }

    @Override
    void flush() {
        getPrintStream().flush()
    }

    @Override
    void close() {
        getPrintStream().close()
    }

    @Override
    boolean checkError() {
        getPrintStream().checkError()
    }

    @Override
    void write(int b) {
        getPrintStream().write(b)
    }

    @Override
    void write(byte[] buf, int off, int len) {
        getPrintStream().write(buf, off, len)
    }

    @Override
    void print(boolean b) {
        getPrintStream().print(b)
    }

    @Override
    void print(char c) {
        getPrintStream().print(c)
    }

    @Override
    void print(int i) {
        getPrintStream().print(i)
    }

    @Override
    void print(long l) {
        getPrintStream().print(l)
    }

    @Override
    void print(float f) {
        getPrintStream().print(f)
    }

    @Override
    void print(double d) {
        getPrintStream().print(d)
    }

    @Override
    void print(char[] s) {
        getPrintStream().print(s)
    }

    @Override
    void print(String s) {
        getPrintStream().print(s)
    }

    @Override
    void print(Object obj) {
        getPrintStream().print(obj)
    }

    @Override
    void println() {
        getPrintStream().println()
    }

    @Override
    void println(boolean x) {
        getPrintStream().println(x)
    }

    @Override
    void println(char x) {
        getPrintStream().println(x)
    }

    @Override
    void println(int x) {
        getPrintStream().println(x)
    }

    @Override
    void println(long x) {
        getPrintStream().println(x)
    }

    @Override
    void println(float x) {
        getPrintStream().println(x)
    }

    @Override
    void println(double x) {
        getPrintStream().println(x)
    }

    @Override
    void println(char[] x) {
        getPrintStream().println(x)
    }

    @Override
    void println(String x) {
        getPrintStream().println(x)
    }

    @Override
    void println(Object x) {
        getPrintStream().println(x)
    }

    @Override
    PrintStream printf(String format, Object... args) {
        getPrintStream().printf(format, args)
    }

    @Override
    PrintStream printf(Locale l, String format, Object... args) {
        getPrintStream().printf(l, format, args)
    }

    @Override
    PrintStream format(String format, Object... args) {
        getPrintStream().format(format, args)
    }

    @Override
    PrintStream format(Locale l, String format, Object... args) {
        getPrintStream().format(l, format, args)
    }

    @Override
    PrintStream append(CharSequence csq) {
        getPrintStream().append()
    }

    @Override
    PrintStream append(CharSequence csq, int start, int end) {
        getPrintStream().append(csq, start, end)
    }

    @Override
    PrintStream append(char c) {
        getPrintStream().append(c)
    }

}

