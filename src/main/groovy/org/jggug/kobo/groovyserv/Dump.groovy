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

class Dump {


  static void dump(OutputStream os, byte[] buf, int ofs, int len) {
    PrintWriter pw = new PrintWriter(os);
    pw.println("+-----------+-----------+-----------+-----------+");

    StringBuilder abuf = new StringBuilder();

    int i;
    for (i=ofs; i<ofs+len; i++) {
      if (!Character.isISOControl((char)buf[i])) {
        abuf.append((char)buf[i])
      }
      else {
        abuf.append("?")
      }
      pw.print(String.format("%02x ", buf[i]));
      if ((i-ofs) % 16 == 15) {
        pw.print("| "+abuf);
        pw.println();
        abuf.length = 0
      }
    }
    if ((len % 16) != 0) {
      while (i < (len+16).intdiv(16)*16 + ofs ) {
        i++;
        pw.print("   ");
      }
      pw.println("| "+abuf);
    }
    pw.flush()
  }

}
