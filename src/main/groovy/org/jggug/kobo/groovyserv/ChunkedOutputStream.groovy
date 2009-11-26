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

class ChunkedOutputStream extends OutputStream {

  final String HEADER_STREAM_ID = "Id";
  final String HEADER_CHUNK_SIZE = "Size";

  char streamIdentifier;

  private final static Object monitor = new Object();
  static WeakHashMap<Thread, OutputStream>map = [:]

  public void flush() throws IOException {
    synchronized (monitor) {
      OutputStream outs = map[Thread.currentThread()]
      if (outs == null) {
        return;
      }
      outs.flush();
    }
  }

  public void close() throws IOException {
    synchronized (monitor) {
      OutputStream outs = map[Thread.currentThread()]
      if (outs == null) {
        return;
      }
      outs.close();
    }
  }

  @Override
  public void write(int b) {
    synchronized (monitor) {
      byte[] buf = new byte[1]
      buf[0] = (b>>8*0) & 0x0000ff;
      write(buf, 0, 1);
    }
  }

  @Override
  public void write(byte[] b, int off, int len) {
    synchronized (monitor) {
      OutputStream outs = map[Thread.currentThread()]
      if (outs == null) {
        return
      }
      if (System.getProperty("groovyserver.verbose") == "true") {
        GroovyServer.originalErr.println("id="+streamIdentifier);
        GroovyServer.originalErr.println("size="+len);
        Dump.dump(GroovyServer.originalErr, b, off, len);
      }
      GroovyServer.originalErr.println("TID="+Thread.currentThread().id);
      outs.write((HEADER_STREAM_ID+": "+streamIdentifier+ "\n").bytes);
      outs.write((HEADER_CHUNK_SIZE+": " + len+"\n").bytes);
      outs.write("\n".bytes);
      outs.write(b, off, len);
    }
  }

  public ChunkedOutputStream(OutputStream outs, char id) {
    map[Thread.currentThread()] = outs
    streamIdentifier = id;
  }

}
