#!/usr/bin/env groovy
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

import org.codehaus.groovy.tools.shell.util.NoExitSecurityManager;
import groovy.ui.GroovyMain;

/**
 * GroovyServer runs groovy command background.
 * This makes groovy response time at startup very quicker.
 *
 * Communication Protocol summary:
 * <pre>
 * [Client -> Server]
 *
 * Cwd: <cwd><CR><LF>
 * Arg: <argn><CR><LF>
 * Arg: <arg1><CR><LF>
 * Arg: <arg2><CR><LF>
 * <CR><LF>
 * <data from stdin><EOF>
 *
 * where
 *  <cwd> is current working directory.
 *  <argn> is number of given commandline arguments
 *         for groovy command.
 *  <arg1><arg2>.. are commandline arguments.
 *  <CR> is carridge return (0x0d ^M).
 *  <LF> is line feed (0x0a, '\n').
 *  <data from stdin> is byte sequence from standard input.
 *
 * [Server -> Client]
 *
 * <id><size><LF>
 * ... chunk data(size bytes) ....
 * <id><size><LF>
 * ... chunk data(size bytes) ....
 * <id><size><LF>
 * ... chunk data(size bytes) ....
 * 
 * where <id> is one of 'o' or 'e'.
 *            'o' means standard output of the program.
 *            'e' means standard error of the program.
 * 
 * </pre>
 *
 * @author UEHARA Junji
 */
class GroovyServer implements Runnable {

  final String HEADER_CURRENT_WORKING_DIR = "Cwd";
  final String HEADER_ARG = "Arg";

  final int CR = 0x0d
  final int LF = 0x0a

  static BufferedInputStream originalIn = System.in
  static OutputStream originalOut = System.out
  static OutputStream originalErr = System.err

  String getLine(InputStream ins) {
    def sb = new StringBuilder()
    char ch;
    while ((ch = ins.read()) != LF as char && ch != -1)  {
      if (ch != CR as char) {
        sb.append(ch)
      }
    }
    sb.toString()
  }

  Map<String, List<String>> readHeaders(ins) {
    def result = [:]
    def line
    while ((line = getLine(ins)) != "") {
      def kv = line.split(':', 2);
      def key = kv[0]
      def value = kv[1]
      if (!result.containsKey(key)) {
        result[key] = []
      }
      if (value.charAt(0) == ' ') {
        value = value.substring(1);
      }
      result[key] += value
    }
    result
  }

  def soc

  void run() {
    try {
      soc.withStreams { ins, outs ->
      originalErr.println "outs="+outs
        Map<String, List<String>> headers = readHeaders(ins);

        def currentDir = headers[HEADER_CURRENT_WORKING_DIR][0]
        System.setProperty('user.dir', currentDir)

        System.setIn(ins);
//        System.setOut(new ChunkedPrintStream(outs, 'o' as char));
//        System.setErr(new ChunkedPrintStream(outs, 'e' as char));
        System.setOut(new PrintStream(new ChunkedOutputStream(outs, 'o' as char)));
        System.setErr(new PrintStream(new ChunkedOutputStream(outs, 'e' as char)));

        try {
          GroovyMain.main(headers[HEADER_ARG] as String[])
        }
        catch (Throwable t) {
          t.printStackTrace()
        }
      }
    }
    finally {
      originalErr.println("socket close")
      soc.close()
    }
  }

  static main(args) {

    def port = 1961;
    // TODO: specify port number with commandline option.
    // But command line include options are pass to
    // original groovy command tranparently. So
    // special option is not good idea(in future,
    // it could conflict to groovy's options).
    // I hope original groovy support like "groovy -s(erver) port"
    // to "run as server" option :).

    if (System.getProperty('groovy.server.port') != null) {
      port = Integer.parseInt(System.getProperty('groovy.server.port'))
    }

    System.setProperty('groovy.runningmode', "server")

    System.setSecurityManager(new NoExitSecurityManager());

    def serverSocket = new ServerSocket(port)

    Thread worker = null;
    while (true) {
      def soc = serverSocket.accept()
      originalErr.println " accept soc="+soc

      // There is no proper way to detect socket disconnection by the client
      // in Socket API. The reason comes from TCP/IP design policy itself.
      // So following is useless (only windows?).
      // TODO:
      // make a 'heart beat' handshake protocol for detect disconnection.
      new Thread("Unconnected thread Killer").start {
        try{
          // if socket is closed by client, stop the worker thred by compulsion.
          while (!soc.isClosed()) {
              Thread.sleep(1000)
          }
          if (worker != null) {
              worker.stop()
          }
        }
        catch (Throwable t) {
          t.printStackTrace(originalErr);
        }
      }

      originalErr.println "accept"
      if (soc.localSocketAddress.address.isLoopbackAddress()) {
        // Now, create new thraed for each connections.
        // Don't use ExecutorService or any thread pool system.
        // Because the output streams are distincted by thread id,
        // so Thread and thread ids can't be reused.
        //
        worker = new Thread(new GroovyServer(soc:soc), "worker").start()
      }
      else {
        System.err.println("allow connection from loopback address only")
      }
    }
  }

}

