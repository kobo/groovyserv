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

  final static String HEADER_STREAM_ID = "Channel";
  final static String HEADER_CHUNK_SIZE = "Size";

  char streamIdentifier;

  static WeakHashMap<Thread, OutputStream>map = [:]

  private OutputStream check(OutputStream outs) {
    if (outs == null) {
      throw new IllegalStateException("System.out/err can't access from this thread: "+Thread.currentThread()+":"+Thread.currentThread().id)
    }
    return outs
  }

  @Override
  public void flush() throws IOException {
    OutputStream outs = check(map[Thread.currentThread()])
    outs.flush();
  }

  @Override
  public void close() throws IOException {
    OutputStream outs = check(map[Thread.currentThread()])
    outs.close();
  }

  @Override
  public void write(int b) {
    byte[] buf = new byte[1]
    buf[0] = (b>>8*0) & 0x0000ff;
    write(buf, 0, 1);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    OutputStream outs = check(map[Thread.currentThread()])
    if (System.getProperty("groovyserver.verbose") == "true") {
      GroovyServer.originalErr.println("id="+streamIdentifier);
      GroovyServer.originalErr.println("size="+len);
      Dump.dump(GroovyServer.originalErr, b, off, len);
    }
    //GroovyServer.originalErr.println("TID="+Thread.currentThread().id);
    outs.write((HEADER_STREAM_ID+": "+streamIdentifier+ "\n").bytes);
    outs.write((HEADER_CHUNK_SIZE+": " + len+"\n").bytes);
    outs.write("\n".bytes);
    outs.write(b, off, len);
  }

  public ChunkedOutputStream(OutputStream outs, char id) {
    map[Thread.currentThread()] = outs
    streamIdentifier = id;
  }

}
