/*
 * Copyright 2009-2013 the original author or authors.
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

import org.jggug.kobo.groovyserv.test.IntegrationTest
import org.jggug.kobo.groovyserv.test.TestUtils
import spock.lang.Specification

/**
 * Specifications for the {@code groovyclient}.
 * Before running this, you must start groovyserver.
 *
 * Because running this test method is too slow,
 * some test cases aggregated to one test method.
 *
 * Each env key includes test method name because
 * avoiding side effects of a previous test case.
 */
@IntegrationTest
class EnvPropagateSpec extends Specification {

    def "specifying -Cenv allows you propagating specified environment variables"() {
        expect:
        assertEnvPropagation([
            // Env of client side
            "HOGE___testEnv_singleOption___KEY1___FOO": "111",
            "HOGE___testEnv_singleOption___KEY1___": "222",
            "___testEnv_singleOption___KEY1___FOO": "333",
            "___testEnv_singleOption___KEY1___": "444",
            "___testEnv_singleOption___key___": "555",
            "___testEnv_singleOption___ABC": "666",
            "___testEnv_singleOption___KEY1___${'_' * 100}": "${'v' * 100}",
            "___testEnv_multiOptions___KEY1___": "777",
            "___testEnv_multiOptions___KEY2___": "888",
            "___testEnv_multiOptions___KEY3___": "999",
        ], [
            "-Cenv", "___testEnv_singleOption___KEY1___",
            "-Cenv", "___testEnv_multiOptions___KEY3___",
            "-e", '''"""
               |assert System.getenv('HOGE___testEnv_singleOption___KEY1___FOO') == '111'      // part match
               |assert System.getenv('HOGE___testEnv_singleOption___KEY1___') == '222'         // startsWith
               |assert System.getenv('___testEnv_singleOption___KEY1___FOO') == '333'          // endsWith
               |assert System.getenv('___testEnv_singleOption___KEY1___') == '444'             // full match
               |assert System.getenv('___testEnv_singleOption___key___') == null               // not match: capital sensitive
               |assert System.getenv('___testEnv_singleOption___ABC') == null                // not match: not including a key
               |assert System.getenv('___testEnv_singleOption___KEY1___' + '_'*100) == 'v'*100 // a long name is OK
               |assert System.getenv('___testEnv_multiOptions___KEY2___') == null              // not match
               |assert System.getenv('___testEnv_multiOptions___KEY3___') == '999'             // matched second option
               |print('OK')
               |"""'''.stripMargin()
        ])
    }

    def "specifying -Cenv-all allows you propagating ALL environment variables"() {
        expect:
        assertEnvPropagation([
            // Env of client side
            "HOGE___testEnvAll___KEY___FOO": "111",
            "HOGE___testEnvAll___KEY___": "222",
            "___testEnvAll___KEY___FOO": "333",
            "___testEnvAll___KEY___": "444",
            "___testEnvAll___key___": "555",
            "___testEnvAll___ABC": "666",
            "___testEnvAll___KEY___${'_' * 100}": "${'v' * 100}",
        ], [
            "-Cenv-all",
            "-e", '''"""
               |assert System.getenv('HOGE___testEnvAll___KEY___FOO') == '111'
               |assert System.getenv('HOGE___testEnvAll___KEY___') == '222'
               |assert System.getenv('___testEnvAll___KEY___FOO') == '333'
               |assert System.getenv('___testEnvAll___KEY___') == '444'
               |assert System.getenv('___testEnvAll___key___') == '555'
               |assert System.getenv('___testEnvAll___ABC') == '666'
               |assert System.getenv('___testEnvAll___KEY___' + '_'*100) == 'v'*100 // a long name is OK
               |print('OK')
               |"""'''.stripMargin()
        ])
    }

    def "specifying -Cenv-exclude prohibits specified environment variables from being propagated (with -Cenv)"() {
        expect:
        assertEnvPropagation([
            // Env of client side
            "EXCLUDE1___testEnv_withEnvExclude___KEY___": "111",
            "___testEnv_withEnvExclude___KEY___EXCLUDE1": "222",
            "HOGEEXCLUDE1___testEnv_withEnvExclude___KEY___": "333",
            "HOGE___testEnv_withEnvExclude___KEY___FOO": "444",
            "___testEnv_withEnvExclude___KEY___": "555",
            "___testEnv_withEnvExclude___ABC": "666",
            "___testEnv_withEnvExclude___KEY___EXCLUDE1": "777",
            "___testEnv_withEnvExclude___KEY___EXCLUDE2": "888",
            "___testEnv_withEnvExclude___KEY___EXCLUDE3": "999",
        ], [
            "-Cenv", "___testEnv_withEnvExclude___KEY___",
            "-Cenv-exclude", "EXCLUDE1",
            "-Cenv-exclude", "EXCLUDE3",
            "-e", '''"""
               |assert System.getenv('EXCLUDE1___testEnv_withEnvExclude___KEY___') == null     // matched to env but excluded
               |assert System.getenv('___testEnv_withEnvExclude___KEY___EXCLUDE1') == null     // matched to env but excluded
               |assert System.getenv('HOGEEXCLUDE1___testEnv_withEnvExclude___KEY___') == null // matched to env but excluded
               |assert System.getenv('HOGE___testEnv_withEnvExclude___KEY___FOO') == '444'
               |assert System.getenv('___testEnv_withEnvExclude___KEY___') == '555'
               |assert System.getenv('___testEnv_withEnvExclude___ABC') == null              // not match to env
               |assert System.getenv('___testEnv_withEnvExclude___KEY___EXCLUDE1') == null     // matched to env but excluded
               |assert System.getenv('___testEnv_withEnvExclude___KEY___EXCLUDE2') == '888'
               |assert System.getenv('___testEnv_withEnvExclude___KEY___EXCLUDE3') == null     // matched to env but excluded
               |print('OK')
               |"""'''.stripMargin()
        ])
    }

    def "specifying -Cenv-exclude prohibits specified environment variables from being propagated (with -Cenv-all)"() {
        expect:
        assertEnvPropagation([
            // Env of client side
            "EXCLUDE___testEnvAll_withEnvExclude___KEY___": "111",
            "___testEnvAll_withEnvExclude___KEY___EXCLUDE": "222",
            "HOGEEXCLUDE___testEnvAll_withEnvExclude___KEY___": "333",
            "HOGE___testEnvAll_withEnvExclude___KEY___FOO": "444",
            "___testEnvAll_withEnvExclude___KEY___": "555",
            "___testEnvAll_withEnvExclude___EXCLUDE": "666",
            "___testEnvAll_withEnvExclude___ABC": "777",
        ], [
            "-Cenv-all",
            "-Cenv-exclude", "EXCLUDE",
            "-e", '''"""
               |assert System.getenv('EXCLUDE___testEnvAll_withEnvExclude___KEY___') == null     // excluded
               |assert System.getenv('___testEnvAll_withEnvExclude___KEY___EXCLUDE') == null     // excluded
               |assert System.getenv('HOGEEXCLUDE___testEnvAll_withEnvExclude___KEY___') == null // excluded
               |assert System.getenv('HOGE___testEnvAll_withEnvExclude___KEY___FOO') == '444'
               |assert System.getenv('___testEnvAll_withEnvExclude___KEY___') == '555'
               |assert System.getenv('___testEnvAll_withEnvExclude___EXCLUDE') == null         // excluded
               |assert System.getenv('___testEnvAll_withEnvExclude___ABC') == '777'
               |print('OK')
               |"""'''.stripMargin()
        ])
    }

    def "propagated environment variables isn't disposed on server"() {
        expect:
        assertEnvPropagation([
            "___testKeepOnServer___KEY1___": "111",
            "___testKeepOnServer___KEY2___": "222"
        ], [
            "-Cenv-all",
            "-e", '''"""
               |assert System.getenv('___testKeepOnServer___KEY1___') == '111'
               |assert System.getenv('___testKeepOnServer___KEY2___') == '222'
               |print('OK')
               |"""'''.stripMargin()
        ])

        and: "can overwrite it"
        assertEnvPropagation([
            "___testKeepOnServer___KEY1___": "XYZ",
        ], [
            "-Cenv-all",
            "-e", '''"""
               |assert System.getenv('___testKeepOnServer___KEY1___') == 'XYZ' // override
               |assert System.getenv('___testKeepOnServer___KEY2___') == '222' // the propagated value is kept
               |print('OK')
               |"""'''.stripMargin()
        ])
    }

    def "you can access to environment variables which is originally existed at server process"() {
        TestUtils.executeClientScriptOkWithEnv(["-e", '"print(System.getenv(\'USER\'))"'], [:]) { p ->
            p.in.text != "null"
            p.err.text == ""
        }
    }

    private static void assertEnvPropagation(Map envMap, List command) {
        TestUtils.executeClientScriptOkWithEnv(command, envMap) { p ->
            assert p.err.text == ""
            assert p.text == "OK"
        }
    }
}
