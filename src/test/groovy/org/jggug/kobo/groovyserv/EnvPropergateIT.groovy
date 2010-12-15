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

usage: groovyclient -C[options for groovyclient] [args/options for groovy]
options:
  -Ch,-Chelp                        Usage information of groovyclient options
  -Cenv=<pattern>                   Pass the environment variables which name
                                    matches with the specified pattern. On the
                                    server process, those variables are set to
                                    or overwitten by the value which the client
                                    process holds.
  -Cenv_all                         Pass all environment vars
  -Cenv_exclude=<pattern>           Don't pass the environment variables which
                                    name matches with pattern
"""

   void testExec_envin_c_option_usage() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
	   def p1 = TestUtils.executeClient(["-Ch"])
	   assert p1.text == usageString

	   def p2 = TestUtils.executeClient(["-Chelp"])
	   assert p2.text == usageString
   }

   void testExec_envin_c_option_merged_usage() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
	   def p1 = TestUtils.executeClient(["--help"])
	   assert p1.text.endsWith(usageString)

	   def p2 = TestUtils.executeClient(["-help"])
	   assert p2.text.endsWith(usageString)

	   def p3 = TestUtils.executeClient(["-h"])
	   assert p3.text.endsWith(usageString)

	   def p4 = TestUtils.executeClient([])
	   assert p4.text.endsWith(usageString)
   }

   void testExec_envin_c_option_usage_on_illegalloption() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
	   def p = TestUtils.executeClient(["-Cxxx", "-e", '"println(System.getenv(\'PATH\'))"'])
	   assert p.text == usageString

	   assert p.err.text == """\
ERROR: unknown option xxx
"""
	   assertEquals 1, p.exitValue()
   }

   void testExec_envin_fullmatch() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=ABCDEF", "-e", '"println(System.getenv(\'ABCDEF\'))"'],
                                              ["ABCDEF=1234"])
	   assert p.err.text == ""
       assert p.text == """1234
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_startsWith() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=GHI", "-e", '"println(System.getenv(\'GHIJK\'))"'],
                                              ["GHIJK=1234"])
	   assert p.err.text == ""
       assert p.text == """1234
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_endsWith() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=NOP", "-e", '"println(System.getenv(\'LMNOP\'))"'],
                                              ["LMNOP=1234"])
	   assert p.err.text == ""
       assert p.text == """1234
"""
	   assertEquals 0, p.exitValue()
   }
   
   void testExec_envin_middle() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=RST", "-e", '"println(System.getenv(\'QRSTU\'))"'],
                                              ["QRSTU=1234"])
	   assert p.err.text == ""
       assert p.text == """1234
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_all() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv_all", "-e", '"println(System.getenv(\'VWXY\'))"'],
                                              ["VWXY=1234"])
	   assert p.err.text == ""
       assert p.text == """1234
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_envnotexist() {
       def p = TestUtils.executeClient(["-e", '"println(System.getenv(\'lkdfeidjifefeyn\')==null)"'])
	   assert p.err.text == ""
       assert p.text == """true
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_multivars_startsWith() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=A", "-e", '"println(System.getenv(\'A01\')+System.getenv(\'A02\'))"'],
                                              ["A01=1234", "A02=5678"])
	   assert p.err.text == ""
       assert p.text == """12345678
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_multivars_endWith() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=Z", "-e", '"println(System.getenv(\'AZ\')+System.getenv(\'BZ\'))"'],
                                              ["AZ=1234", "BZ=5678"])
	   assert p.err.text == ""
       assert p.text == """12345678
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_multivars_middle() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=X", "-e", '"println(System.getenv(\'AXZ\')+System.getenv(\'BXZ\'))"'],
                                              ["AXZ=1234", "BXZ=5678"])
	   assert p.err.text == ""
       assert p.text == """12345678
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envin_long() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def varname = 'X' * 100
       def varvalue = 'Y' * 100
       def p = TestUtils.executeClientWithEnv(["-Cenv=$varname", "-e", '"println(System.getenv(\''+varname+'\'))"'],
                                              ["$varname=$varvalue"])
	   assert p.err.text == ""
       assert p.text == """$varvalue
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envex() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=X", "-Cenv_exclude=X01", "-e", '"println(System.getenv(\'X01\')+System.getenv(\'X02\'))"'],
                                              ["X01=1234", "X02=5678"])
	   assert p.err.text == ""
       assert p.text == """null5678
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envex_same() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv=X03", "-Cenv_exclude=X03", "-e", '"println(System.getenv(\'X03\'))"'],
                                              ["X03=1234"])
	   assert p.err.text == ""
       assert p.text == """null
"""
	   assertEquals 0, p.exitValue()

   }

   void testExec_envex_with_all() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv_all", "-Cenv_exclude=X04", "-e", '"println(System.getenv(\'X04\')+System.getenv(\'X05\'))"'],
                                              ["X04=1234", "X05=5678"])
	   assert p.err.text == ""
       assert p.text == """null5678
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_envall() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p = TestUtils.executeClientWithEnv(["-Cenv_all", "-e", '"println(System.getenv(\'X06\'))"'],
                                              ["X06=1234"])
	   assert p.err.text == ""
       assert p.text == """1234
"""
	   assertEquals 0, p.exitValue()
   }

   void testExec_env_keep_and_overwrite() {
       if (System.getProperty('groovyservClientExecutable')?.endsWith('.rb')) { // TODO: not implement this feature on ruby client.
           return
       }
       def p1 = TestUtils.executeClientWithEnv(["-Cenv_all", "-e", '"print(System.getenv(\'Y01\'))"'], ["Y01=1234"])
	   assert p1.err.text == ""
       assert p1.text == "1234"
	   assertEquals 0, p1.exitValue()

       // keep
       def p2 = TestUtils.executeClientWithEnv(["-Cenv_all", "-e", '"print(System.getenv(\'Y01\'))"'])
	   assert p2.err.text == ""
       assert p2.text == "1234"
	   assertEquals 0, p2.exitValue()

       // overwrite
       def p3 = TestUtils.executeClientWithEnv(["-Cenv_all", "-e", '"print(System.getenv(\'Y01\'))"'], ["Y01=5678"])
	   assert p3.err.text == ""
       assert p3.text == "5678"
	   assertEquals 0, p3.exitValue()

       // keep
       def p4 = TestUtils.executeClientWithEnv(["-Cenv_all", "-e", '"print(System.getenv(\'Y01\'))"'])
	   assert p4.err.text == ""
       assert p4.text == "5678"
	   assertEquals 0, p4.exitValue()
   }
   
   
}
