package org.jggug.kobo.groovyserv

import com.sun.jna.*;
import com.sun.jna.win32.*;

interface CLibrary extends Library {
  String libname = (Platform.isWindows() ? "msvcrt" : "c");
  CLibrary INSTANCE = Native.loadLibrary(libname, CLibrary.class);
  int chdir(String dir);
  int _chdir(String dir);
}

class PlatformMethods {
  static chdir(String dir) {
    if (Platform.isWindows()) {
      CLibrary.INSTANCE._chdir(dir);
    }
    else {
      CLibrary.INSTANCE.chdir(dir);
    }
  }
}
