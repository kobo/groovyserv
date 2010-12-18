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

   static final String SEP = System.getProperty("line.separator")

   static final String usageString = """\

usage: groovyclient -C[option for groovyclient] [args/options for groovy]
options:
  -Ch,-Chelp                       Usage information of groovyclient options
  -Cenv <pattern>                  Pass the environment variables which name
                                   matches with the specified pattern. The values
                                   of matched variables on the client process are
                                   sent to the server process, and the values of
                                   same name environment variable on the server
                                   are set to or overwitten by the passed values.
  -Cenv-all                        Pass all environment variables
  -Cenv-exclude <pattern>          Don't pass the environment variables which
                                   name matches with specified pattern
"""

   void testUsage() {
       def p1 = TestUtils.executeClient(["-Ch"])
       assert p1.text.startsWith("\nusage:")

       def p2 = TestUtils.executeClient(["-Chelp"])
       assert p2.text.startsWith("\nusage:")
   }

   void testUsage_merged() {
       def p0 = TestUtils.executeClient(["-Ch"])
       def clientHelpMessage = p0.text

       def p1 = TestUtils.executeClient(["--help"])
       assert p1.text.endsWith(clientHelpMessage)

       def p2 = TestUtils.executeClient(["-help"])
       assert p2.text.endsWith(clientHelpMessage)

       def p3 = TestUtils.executeClient(["-h"])
       assert p3.text.endsWith(clientHelpMessage)

       def p4 = TestUtils.executeClient([])
       assert p4.text.endsWith(clientHelpMessage)
   }

   void testUsage_on_illegal_option() {
       def p0 = TestUtils.executeClient(["-Ch"])
       def clientHelpMessage = p0.text
       def p = TestUtils.executeClient(["-Cxxx",
                                        "-e", '"println(System.getenv(\'PATH\'))"'])
       def s = p.text
       assert s == clientHelpMessage

       assert p.err.text == "ERROR: unknown option -Cxxx\n"
       assertEquals 1, p.exitValue()
   }

   void testEnv_fullmatch() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "ABCDEF",
                                               "-e", '"print(System.getenv(\'ABCDEF\'))"'],
                                              ["ABCDEF=1234"])
       assert p.err.text == ""
       assert p.text == "1234"
       assertEquals 0, p.exitValue()
   }

   void testEnv_startsWith() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "GHI",
                                               "-e", '"print(System.getenv(\'GHIJK\'))"'],
                                              ["GHIJK=1234"])
       assert p.err.text == ""
       assert p.text == "1234"
       assertEquals 0, p.exitValue()
   }

   void testEnv_endsWith() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "NOP", "-e", '"print(System.getenv(\'LMNOP\'))"'],
                                              ["LMNOP=1234"])
       assert p.err.text == ""
       assert p.text == "1234"
       assertEquals 0, p.exitValue()
   }

   void testEnv_onMiddle() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "RST",
                                               "-e", '"print(System.getenv(\'QRSTU\'))"'],
                                              ["QRSTU=1234"])
       assert p.err.text == ""
       assert p.text == "1234"
       assertEquals 0, p.exitValue()
   }

   void testEnv_all() {
       def p = TestUtils.executeClientWithEnv(["-Cenv-all",
                                               "-e", '"print(System.getenv(\'VWXY\'))"'],
                                              ["VWXY=1234"])
       assert p.err.text == ""
       assert p.text == "1234"
       assertEquals 0, p.exitValue()
   }

   void testEnv_not_exist_var() {
       def p = TestUtils.executeClient(["-e", '"print(System.getenv(\'lkdfeidjifefeyn\')==null)"'])
       assert p.err.text == ""
       assert p.text == "true"
       assertEquals 0, p.exitValue()
   }

   void testEnv_multivars_startsWith() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "A",
                                               "-e", '"print(System.getenv(\'A01\')+System.getenv(\'A02\'))"'],
                                              ["A01=1234", "A02=5678"])
       assert p.err.text == ""
       assert p.text == "12345678"
       assertEquals 0, p.exitValue()
   }

   void testEnv_multivars_endWith() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "Z",
                                               "-e", '"print(System.getenv(\'AZ\')+System.getenv(\'BZ\'))"'],
                                              ["AZ=1234", "BZ=5678"])
       assert p.err.text == ""
       assert p.text == "12345678"
       assertEquals 0, p.exitValue()
   }

   void testEnv_multivars_middle() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "X", "-e", '"print(System.getenv(\'AXZ\')+System.getenv(\'BXZ\'))"'],
                                              ["AXZ=1234", "BXZ=5678"])
       assert p.err.text == ""
       assert p.text == "12345678"
       assertEquals 0, p.exitValue()
   }

   void testEnv_long_var_name() {
       def varname = 'X' * 100
       def varvalue = 'Y' * 100
       def p = TestUtils.executeClientWithEnv(["-Cenv", "$varname",
                                               "-e", '"print(System.getenv(\''+varname+'\'))"'],
                                              ["$varname=$varvalue"])
       assert p.err.text == ""
       assert p.text == "$varvalue"
       assertEquals 0, p.exitValue()
   }

   void testEnv_many_values() {
       def arg = []
       (1..10).eachWithIndex { it, idx ->
           arg += ["-Cenv", "_VAR$it" ]
       }
       def p = TestUtils.executeClient([*arg, "-e", '"print(\'hello\')"'])
       assert p.text == "hello"
       assert p.err.text == ""
       assertEquals 0, p.exitValue()
   }

   void testEnv_too_many_values() {
       def arg = []
       (1..11).eachWithIndex { it, idx ->
           arg += ["-Cenv", "_VAR$it" ]
       }
       def p = TestUtils.executeClient([*arg, "-e", '"print(\'hello\')"'])
       if (!System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) {
           assert p.err.text == "ERROR: too many option: env _VAR11\n"
           assert p.text.startsWith("\nusage:")
           assertEquals 1, p.exitValue()
       }
       else {
           // ruby client has no limitation about number of patterns.
           assert p.text == "hello"
           assert p.err.text == ""
           assertEquals 0, p.exitValue()
       }
   }

   void testEnv_exclude() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "X", "-Cenv-exclude", "X01",
                                               "-e", '"print(System.getenv(\'X01\')+System.getenv(\'X02\'))"'],
                                              ["X01=1234", "X02=5678"])
       assert p.err.text == ""
       assert p.text == "null5678"
       assertEquals 0, p.exitValue()
   }

   void testEnv_and_exclude() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "X03", "-Cenv-exclude", "X03",
                                               "-e", '"print(System.getenv(\'X03\'))"'],
                                              ["X03=1234"])
       assert p.err.text == ""
       assert p.text == "null"
       assertEquals 0, p.exitValue()

   }

   void testEnv_all_and_exclude() {
       def p = TestUtils.executeClientWithEnv(["-Cenv-all", "-Cenv-exclude", "X04",
                                               "-e", '"print(System.getenv(\'X04\')+System.getenv(\'X05\'))"'],
                                              ["X04=1234", "X05=5678"])
       assert p.err.text == ""
       assert p.text == "null5678"
       assertEquals 0, p.exitValue()
   }

   void testEnv_and_exclude_multivars() {
       def p = TestUtils.executeClientWithEnv(["-Cenv", "X", "-Cenv", "Y",
                                               "-Cenv-exclude", "XX",  "-Cenv-exclude", "YY",
                                               "-e", '"print(System.getenv(\'X07\')+System.getenv(\'Y07\')+System.getenv(\'XX\')+System.getenv(\'YY\'))"'],
                                              ["X07=1234", "Y07=5678", "XX=abcd", "YY=efgh"])
       assert p.err.text == ""
       assert p.text == "12345678nullnull"
       assertEquals 0, p.exitValue()
   }

   void testEnv_keep_and_overwrite() {
       def p1 = TestUtils.executeClientWithEnv(["-Cenv-all",
                                                "-e", '"print(System.getenv(\'Y01\'))"'], ["Y01=1234"])
       assert p1.err.text == ""
       assert p1.text == "1234"
       assertEquals 0, p1.exitValue()

       // keep
       def p2 = TestUtils.executeClient(["-Cenv-all",
                                         "-e", '"print(System.getenv(\'Y01\'))"'])
       assert p2.err.text == ""
       assert p2.text == "1234"
       assertEquals 0, p2.exitValue()

       // overwrite
       def p3 = TestUtils.executeClientWithEnv(["-Cenv-all",
                                                "-e", '"print(System.getenv(\'Y01\'))"'], ["Y01=5678"])
       assert p3.err.text == ""
       assert p3.text == "5678"
       assertEquals 0, p3.exitValue()

       // keep
       def p4 = TestUtils.executeClient(["-Cenv-all",
                                         "-e", '"print(System.getenv(\'Y01\'))"'])
       assert p4.err.text == ""
       assert p4.text == "5678"
       assertEquals 0, p4.exitValue()
   }

   void testEnv_protect_and_overwrite() {
       // initialize
       def p1 = TestUtils.executeClientWithEnv(["-Cenv-all",
                                                "-e", '"print(System.getenv(\'Y04\')+System.getenv(\'Y05\'))"'],
                                               ["Y04=1234", "Y05=5678"])
       assert p1.err.text == ""
       assert p1.text == "12345678"
       assertEquals 0, p1.exitValue()

       // protect Y05 but replace Y04(1234->abcd)
       def p2 = TestUtils.executeClientWithEnv(["-Cenv", "Y04", "-Cenv-exclude", "Y05",
                                                "-e", '"print(System.getenv(\'Y04\')+System.getenv(\'Y05\'))"'],
                                               ["Y04=abcd", "Y05=efgh"])

       assert p2.err.text == ""
       assert p2.text == "abcd5678"
       assertEquals 0, p2.exitValue()
   }

   void testEnv_require_param() {
       def p = TestUtils.executeClient(['"print(System.getenv(\'X06\'))"', "-Cenv" ])
       assert p.err.text == "ERROR: option -Cenv require param\n"
       assert p.text.startsWith("\nusage:")
       assertEquals 1, p.exitValue()
   }

}
