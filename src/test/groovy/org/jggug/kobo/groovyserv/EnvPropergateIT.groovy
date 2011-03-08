/*
* Copyright 2009-2010 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.jggug.kobo.groovyserv

import groovy.util.GroovyTestCase

/**
* Tests for the {@code groovyclient}.
* Before running this, you must start groovyserver.
*/
class EnvPropergateIT extends GroovyTestCase {

    void testUsage() {
        TestUtils.executeClient(["-Ch"]) {
            assert it.text.startsWith("\nusage:")
        }
        TestUtils.executeClient(["-Chelp"]) {
            assert it.text.startsWith("\nusage:")
        }
   }

   void testUsage_merged() {

       if (com.sun.jna.Platform.isWindows()) { // TODO: fix this case
           return
       }

       def clientHelpMessage
       TestUtils.executeClient(["-Ch"]) { clientHelpMessage = it.text }
       TestUtils.executeClient(["--help"]) {
           def s = it.text
           assert s.endsWith(clientHelpMessage)
       }

       TestUtils.executeClient(["-help"]) {
           assert it.text.endsWith(clientHelpMessage)
       }

       TestUtils.executeClient(["-h"]) {
           assert it.text.endsWith(clientHelpMessage)
       }

       TestUtils.executeClient([]) {
           assert it.text.endsWith(clientHelpMessage)
       }
   }

   void testUsage_on_illegal_option() {
       def clientHelpMessage
       TestUtils.executeClient(["-Ch"]) {
           clientHelpMessage = it.text
       }

       def p = TestUtils.executeClient(["-Cxxx",
                                        "-e", '"println(System.getenv(\'PATH\'))"']) {
           assert clientHelpMessage == it.text
           assert it.err.text.startsWith("ERROR: unknown option -Cxxx")
       }
       assert p.exitValue() == 1
   }

   void testEnv_fullmatch() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "ABCDEF",
                                         "-e", '"print(System.getenv(\'ABCDEF\'))"'],
                                        ["ABCDEF=1234"]) {
           assert it.err.text == ""
           assert it.text == "1234"
       }
   }

   void testEnv_startsWith() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "GHI",
                                       "-e", '"print(System.getenv(\'GHIJK\'))"'],
                                      ["GHIJK=1234"]) {
           
           assert it.err.text == ""
           assert it.text == "1234"
       }
   }

   void testEnv_endsWith() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "NOP", "-e", '"print(System.getenv(\'LMNOP\'))"'],
                                      ["LMNOP=1234"]) {
           assert it.err.text == ""
           assert it.text == "1234"
       }
   }

   void testEnv_onMiddle() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "RST",
                                       "-e", '"print(System.getenv(\'QRSTU\'))"'],
                                      ["QRSTU=1234"]) {
           assert it.err.text == ""
           assert it.text == "1234"
       }
   }

   void testEnv_all() {
       TestUtils.executeClientOkWithEnv(["-Cenv-all",
                                       "-e", '"print(System.getenv(\'VWXY\'))"'],
                                      ["VWXY=1234"]) {
           assert it.err.text == ""
           assert it.text == "1234"
       }
   }

   void testEnv_not_exist_var() {
       TestUtils.executeClientOk(["-e", '"print(System.getenv(\'lkdfeidjifefeyn\')==null)"']) {
           assert it.err.text == ""
           assert it.text == "true"
       }
   }

   void testEnv_multivars_startsWith() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "A",
                                       "-e", '"print(System.getenv(\'A01\')+System.getenv(\'A02\'))"'],
                                      ["A01=1234", "A02=5678"]) {
           assert it.err.text == ""
           assert it.text == "12345678"
       }
   }

   void testEnv_multivars_endWith() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "Z",
                                       "-e", '"print(System.getenv(\'AZ\')+System.getenv(\'BZ\'))"'],
                                      ["AZ=1234", "BZ=5678"]) {
           assert it.err.text == ""
           assert it.text == "12345678"
       }
   }

   void testEnv_multivars_middle() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "X", "-e", '"print(System.getenv(\'AXZ\')+System.getenv(\'BXZ\'))"'],
                                      ["AXZ=1234", "BXZ=5678"]) {
           assert it.err.text == ""
           assert it.text == "12345678"
       }
   }

   void testEnv_long_var_name() {
       def varname = 'X' * 100
       def varvalue = 'Y' * 100
       TestUtils.executeClientOkWithEnv(["-Cenv", "$varname",
                                       "-e", '"print(System.getenv(\''+varname+'\'))"'],
                                      ["$varname=$varvalue"]) {
           assert it.err.text == ""
           assert it.text == "$varvalue"
       }
   }

   void testEnv_many_values() {
       def arg = []
       (1..10).eachWithIndex { it, idx ->
           arg += ["-Cenv", "_VAR$it" ]
       }
       TestUtils.executeClientOk([*arg, "-e", '"print(\'hello\')"']) {
           assert it.text == "hello"
           assert it.err.text == ""
       }
   }

   void testEnv_too_many_values() {
       def arg = []
       (1..11).eachWithIndex { it, idx ->
           arg += ["-Cenv", "_VAR$it" ]
       }
       def expectedExitValue
       def p = TestUtils.executeClient([*arg, "-e", '"print(\'hello\')"']) {
           if (!System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) {
               assert it.text.startsWith("\nusage:")
               assert it.err.text.startsWith("ERROR: too many option: env _VAR11")
               expectedExitValue = 1
           }
           else {
               // ruby client has no limitation about number of patterns.
               assert it.text == "hello"
               assert it.err.text == ""
               expectedExitValue = 0

           }
       }
       assert p.exitValue() == expectedExitValue
   }

   void testEnv_exclude() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "X", "-Cenv-exclude", "X01",
                                       "-e", '"print(System.getenv(\'X01\')+System.getenv(\'X02\'))"'],
                                      ["X01=1234", "X02=5678"]) {
           assert it.text == "null5678"
           assert it.err.text == ""
       }
   }

   void testEnv_and_exclude() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "X03", "-Cenv-exclude", "X03",
                                       "-e", '"print(System.getenv(\'X03\'))"'],
                                      ["X03=1234"]) {
           assert it.text == "null"
           assert it.err.text == ""
       }
   }

   void testEnv_all_and_exclude() {
       TestUtils.executeClientOkWithEnv(["-Cenv-all", "-Cenv-exclude", "X04",
                                       "-e", '"print(System.getenv(\'X04\')+System.getenv(\'X05\'))"'],
                                      ["X04=1234", "X05=5678"]) {
           assert it.text == "null5678"
           assert it.err.text == ""
       }
   }

   void testEnv_and_exclude_multivars() {
       TestUtils.executeClientOkWithEnv(["-Cenv", "X", "-Cenv", "Y",
                                       "-Cenv-exclude", "XX",  "-Cenv-exclude", "YY",
                                       "-e", '"print(System.getenv(\'X07\')+System.getenv(\'Y07\')+System.getenv(\'XX\')+System.getenv(\'YY\'))"'],
                                      ["X07=1234", "Y07=5678", "XX=abcd", "YY=efgh"]) {
           assert it.text == "12345678nullnull"
           assert it.err.text == ""
       }
   }

   void testEnv_keep_and_overwrite() {
       TestUtils.executeClientOkWithEnv(["-Cenv-all",
                                       "-e", '"print(System.getenv(\'Y01\'))"'], ["Y01=1234"]) {
           assert it.text == "1234"
           assert it.err.text == ""
       }

       // keep
       TestUtils.executeClientOk(["-Cenv-all",
                                "-e", '"print(System.getenv(\'Y01\'))"']) {
           assert it.text == "1234"
           assert it.err.text == ""
       }

       // overwrite
       TestUtils.executeClientOkWithEnv(["-Cenv-all",
                                       "-e", '"print(System.getenv(\'Y01\'))"'], ["Y01=5678"]) {
           assert it.text == "5678"
           assert it.err.text == ""
       }

       // keep
       TestUtils.executeClientOk(["-Cenv-all",
                                "-e", '"print(System.getenv(\'Y01\'))"']) {
           assert it.text == "5678"
           assert it.err.text == ""
       }
   }

   void testEnv_protect_and_overwrite() {
       // initialize
       TestUtils.executeClientOkWithEnv(["-Cenv-all",
                                       "-e", '"print(System.getenv(\'Y04\')+System.getenv(\'Y05\'))"'],
                                      ["Y04=1234", "Y05=5678"]) {
           assert it.text == "12345678"
           assert it.err.text == ""
       }

       // protect Y05 but replace Y04(1234->abcd)
       TestUtils.executeClientOkWithEnv(["-Cenv", "Y04", "-Cenv-exclude", "Y05",
                                       "-e", '"print(System.getenv(\'Y04\')+System.getenv(\'Y05\'))"'],
                                      ["Y04=abcd", "Y05=efgh"]) {
           assert it.text == "abcd5678"
           assert it.err.text == ""
       }
   }

   void testEnv_require_param() {
       def p = TestUtils.executeClient(['"print(System.getenv(\'X06\'))"', "-Cenv" ]) {
           assert it.text.startsWith("\nusage:")
           assert it.err.text.startsWith("ERROR: option -Cenv require param")
       }
       assert p.exitValue() == 1
       
   }

}
