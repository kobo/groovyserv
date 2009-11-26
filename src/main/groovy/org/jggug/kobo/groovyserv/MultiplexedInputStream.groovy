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
package org.jggug.kobo.groovyserv

class MultiplexedInputStream extends InputStream {

  static WeakHashMap<Thread, InputStream>map = [:]

  private InputStream check(InputStream ins) {
    if (ins == null) {
      throw new IllegalStateException("System.in can't access from this thread: "+Thread.currentThread()+":"+Thread.currentThread().id)
    }
    return ins
  }


  @Override
  public int read() throws IOException {
    InputStream ins = check(map[Thread.currentThread()])
    return ins.read()
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    InputStream ins = check(map[Thread.currentThread()])
    return ins.read(b, off, len)
  }

  @Override
  public int available() throws IOException {
    InputStream ins = check(map[Thread.currentThread()])
    return ins.available()
  }

  @Override
  public void close() throws IOException {
    InputStream ins = check(map[Thread.currentThread()])
    ins.close()
  }

  @Override
  public void mark(int readlimit) {
    InputStream ins = check(map[Thread.currentThread()])
    ins.mark()
  }

  @Override
  public void reset() throws IOException {
    InputStream ins = check(map[Thread.currentThread()])
    ins.reset()
  }

  @Override
  public boolean markSupported() {
    InputStream ins = check(map[Thread.currentThread()])
    ins.markSupported()
  }


  public MultiplexedInputStream(InputStream ins) {
    map[Thread.currentThread()] = ins
  }

}
