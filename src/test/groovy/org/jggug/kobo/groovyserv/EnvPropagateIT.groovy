/*
 * Copyright 2009-2011 the original author or authors.
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

/**
 * Tests for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 *
 * Because running this test method is too slow,
 * some test cases aggregated to one test method.
 *
 * Each env key includes test method name because
 * avoiding effect of a previous test case.
 */
class EnvPropagationIT extends GroovyTestCase {

   private static void assertEnvPropagation(List env, List command) {
       TestUtils.executeClientOkWithEnv(command, env) {
           assert it.err.text == ""
           assert it.text == "OK"
       }
   }

   void testEnv() {
       assertEnvPropagation([
           // Env of client side
           "HOGE(testEnv_singleOption:KEY1)FOO=111",
           "HOGE(testEnv_singleOption:KEY1)=222",
           "(testEnv_singleOption:KEY1)FOO=333",
           "(testEnv_singleOption:KEY1)=444",
           "(testEnv_singleOption:key)=555",
           "(testEnv_singleOption)ABC=666",
           "(testEnv_singleOption:KEY1)${'_'*100}=${'v'*100}",
           "(testEnv_multiOptions:KEY1)=777",
           "(testEnv_multiOptions:KEY2)=888",
           "(testEnv_multiOptions:KEY3)=999",
       ], [
           "-Cenv", "(testEnv_singleOption:KEY1)",
           "-Cenv", "(testEnv_multiOptions:KEY3)",
           "-e", '''"""
               |assert System.getenv('HOGE(testEnv_singleOption:KEY1)FOO') == '111'      // part match
               |assert System.getenv('HOGE(testEnv_singleOption:KEY1)') == '222'         // startsWith
               |assert System.getenv('(testEnv_singleOption:KEY1)FOO') == '333'          // endsWith
               |assert System.getenv('(testEnv_singleOption:KEY1)') == '444'             // full match
               |assert System.getenv('(testEnv_singleOption:key)') == null               // not match: capital sensitive
               |assert System.getenv('(testEnv_singleOption)ABC') == null                // not match: not including a key
               |assert System.getenv('(testEnv_singleOption:KEY1)' + '_'*100) == 'v'*100 // a long name is OK
               |assert System.getenv('(testEnv_multiOptions:KEY2)') == null              // not match
               |assert System.getenv('(testEnv_multiOptions:KEY3)') == '999'             // matched second option
               |print('OK')
               |"""'''.stripMargin()
       ])
   }

   void testEnvAll() {
       assertEnvPropagation([
           // Env of client side
           "HOGE(testEnvAll:KEY)FOO=111",
           "HOGE(testEnvAll:KEY)=222",
           "(testEnvAll:KEY)FOO=333",
           "(testEnvAll:KEY)=444",
           "(testEnvAll:key)=555",
           "(testEnvAll)ABC=666",
           "(testEnvAll:KEY)${'_'*100}=${'v'*100}",
       ], [
           "-Cenv-all",
           "-e", '''"""
               |assert System.getenv('HOGE(testEnvAll:KEY)FOO') == '111'
               |assert System.getenv('HOGE(testEnvAll:KEY)') == '222'
               |assert System.getenv('(testEnvAll:KEY)FOO') == '333'
               |assert System.getenv('(testEnvAll:KEY)') == '444'
               |assert System.getenv('(testEnvAll:key)') == '555'
               |assert System.getenv('(testEnvAll)ABC') == '666'
               |assert System.getenv('(testEnvAll:KEY)' + '_'*100) == 'v'*100 // a long name is OK
               |print('OK')
               |"""'''.stripMargin()
       ])
   }

   void testEnv_withEnvExclude() {
       assertEnvPropagation([
           // Env of client side
           "EXCLUDE1(testEnv_withEnvExclude:KEY)=111",
           "(testEnv_withEnvExclude:KEY)EXCLUDE1=222",
           "HOGEEXCLUDE1(testEnv_withEnvExclude:KEY)=333",
           "HOGE(testEnv_withEnvExclude:KEY)FOO=444",
           "(testEnv_withEnvExclude:KEY)=555",
           "(testEnv_withEnvExclude)ABC=666",
           "(testEnv_withEnvExclude:KEY)EXCLUDE1=777",
           "(testEnv_withEnvExclude:KEY)EXCLUDE2=888",
           "(testEnv_withEnvExclude:KEY)EXCLUDE3=999",
       ], [
           "-Cenv", "(testEnv_withEnvExclude:KEY)",
           "-Cenv-exclude", "EXCLUDE1",
           "-Cenv-exclude", "EXCLUDE3",
           "-e", '''"""
               |assert System.getenv('EXCLUDE1(testEnv_withEnvExclude:KEY)') == null     // matched to env but excluded
               |assert System.getenv('(testEnv_withEnvExclude:KEY)EXCLUDE1') == null     // matched to env but excluded
               |assert System.getenv('HOGEEXCLUDE1(testEnv_withEnvExclude:KEY)') == null // matched to env but excluded
               |assert System.getenv('HOGE(testEnv_withEnvExclude:KEY)FOO') == '444'
               |assert System.getenv('(testEnv_withEnvExclude:KEY)') == '555'
               |assert System.getenv('(testEnv_withEnvExclude)ABC') == null              // not match to env
               |assert System.getenv('(testEnv_withEnvExclude:KEY)EXCLUDE1') == null     // matched to env but excluded
               |assert System.getenv('(testEnv_withEnvExclude:KEY)EXCLUDE2') == '888'
               |assert System.getenv('(testEnv_withEnvExclude:KEY)EXCLUDE3') == null     // matched to env but excluded
               |print('OK')
               |"""'''.stripMargin()
       ])
   }

   void testEnvAll_withEnvExclude() {
       assertEnvPropagation([
           // Env of client side
           "EXCLUDE(testEnvAll_withEnvExclude:KEY)=111",
           "(testEnvAll_withEnvExclude:KEY)EXCLUDE=222",
           "HOGEEXCLUDE(testEnvAll_withEnvExclude:KEY)=333",
           "HOGE(testEnvAll_withEnvExclude:KEY)FOO=444",
           "(testEnvAll_withEnvExclude:KEY)=555",
           "(testEnvAll_withEnvExclude)EXCLUDE=666",
           "(testEnvAll_withEnvExclude)ABC=777",
       ], [
           "-Cenv-all",
           "-Cenv-exclude", "EXCLUDE",
           "-e", '''"""
               |assert System.getenv('EXCLUDE(testEnvAll_withEnvExclude:KEY)') == null     // excluded
               |assert System.getenv('(testEnvAll_withEnvExclude:KEY)EXCLUDE') == null     // excluded
               |assert System.getenv('HOGEEXCLUDE(testEnvAll_withEnvExclude:KEY)') == null // excluded
               |assert System.getenv('HOGE(testEnvAll_withEnvExclude:KEY)FOO') == '444'
               |assert System.getenv('(testEnvAll_withEnvExclude:KEY)') == '555'
               |assert System.getenv('(testEnvAll_withEnvExclude)EXCLUDE') == null         // excluded
               |assert System.getenv('(testEnvAll_withEnvExclude)ABC') == '777'
               |print('OK')
               |"""'''.stripMargin()
       ])
   }

   void testKeepOnServer() {
       assertEnvPropagation([
           "(testKeepOnServer:KEY1)=111",
           "(testKeepOnServer:KEY2)=222"
       ], [
           "-Cenv-all",
           "-e", '''"""
               |assert System.getenv('(testKeepOnServer:KEY1)') == '111'
               |assert System.getenv('(testKeepOnServer:KEY2)') == '222'
               |print('OK')
               |"""'''.stripMargin()
       ])

       // overwrite
       assertEnvPropagation([
           "(testKeepOnServer:KEY1)=XYZ",
       ], [
           "-Cenv-all",
           "-e", '''"""
               |assert System.getenv('(testKeepOnServer:KEY1)') == 'XYZ' // override
               |assert System.getenv('(testKeepOnServer:KEY2)') == '222' // keep
               |print('OK')
               |"""'''.stripMargin()
       ])
   }

   void testAccessableToOriginalEnvironmentOnServerSides() {
       TestUtils.executeClientOk(["-e", '"print(System.getenv(\'PWD\'))"']) {
           assert it.in.text != "null"
           assert it.err.text == ""
       }
   }

}
