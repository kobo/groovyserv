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

import static org.jggug.kobo.groovyserv.GroovyServer.originalErr;

class MultiplexedInputStream extends InputStream {

  static WeakHashMap<ThreadGroup, InputStream>map = [:]

  private InputStream check(InputStream ins) {
    if (ins == null) {
      throw new IllegalStateException("System.in can't access from this thread: "+Thread.currentThread()+":"+Thread.currentThread().id)
    }
    return ins;
  }

  @Override
  public int read() throws IOException {
    InputStream ins = check(map[Thread.currentThread().getThreadGroup()])
    int result = ins.read();
    if (result != -1 && System.getProperty("groovyserver.verbose") == "true") {
      byte[] b = [result];
      if (System.getProperty("groovyserver.verbose") == "true") {
        GroovyServer.originalErr.println("Client==>Server");
      }
      Dump.dump(GroovyServer.originalErr, b, 0, 1);
    }
    return result;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    InputStream ins = check(map[Thread.currentThread().getThreadGroup()])
    int result = ins.read(b, off, len);
    if (result != 0 && System.getProperty("groovyserver.verbose") == "true") {
      GroovyServer.originalErr.println("Client==>Server");
      GroovyServer.originalErr.println(" id=in");
      GroovyServer.originalErr.println(" size="+result);
      Dump.dump(GroovyServer.originalErr, b, off, result);
    }
    return result;
  }

  @Override
  public int available() throws IOException {
    InputStream ins = check(map[Thread.currentThread().getThreadGroup()])
    return ins.available()
  }

  @Override
  public void close() throws IOException {
    InputStream ins = check(map[Thread.currentThread().getThreadGroup()])
    ins.close()
  }

  @Override
  public void mark(int readlimit) {
    InputStream ins = check(map[Thread.currentThread().getThreadGroup()])
    ins.mark()
  }

  @Override
  public void reset() throws IOException {
    InputStream ins = check(map[Thread.currentThread().getThreadGroup()])
    ins.reset()
  }

  @Override
  public boolean markSupported() {
    InputStream ins = check(map[Thread.currentThread().getThreadGroup()])
    ins.markSupported()
  }

  public MultiplexedInputStream(InputStream ins) {
    def pos = new PipedOutputStream()
    def pis = new PipedInputStream(pos)
    Thread streamCopyWorker = new Thread({
        try{
          while (true) {
            def headers = GroovyServer.readHeaders(ins)
            def sizeHeader = headers[ChunkedOutputStream.HEADER_SIZE]
            if (sizeHeader != null) {
              def size = Integer.parseInt(sizeHeader[0])
              if (size == 0) {
                pos.close()
                return;
              }
              for (int i=0; i<size; i++) {
                int ch = ins.read()
                if (ch == -1) {
                  break;
                }
                pos.write(ch);
              }
              pos.flush();
            }
          }
        }
        catch (java.net.SocketException e) {
          if (System.getProperty("groovyserver.verbose") == "true") {
            GroovyServer.originalErr.println("ins closed.");
          }
        }
        catch (Throwable t) {
          if (System.getProperty("groovyserver.verbose") == "true") {
            GroovyServer.originalErr.println("t="+t);
          }
        }
      } as Runnable)
    map[Thread.currentThread().getThreadGroup()] = pis
    streamCopyWorker.setDaemon(true)
    streamCopyWorker.start();
  }

}
