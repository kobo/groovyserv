/*
 * Copyright 2009-2013 the original author or authors.
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
package org.jggug.kobo.groovyserv.platform

import org.jggug.kobo.groovyserv.test.UnitTest
import spock.lang.Specification

/**
 * Specifications for the {@link org.jggug.kobo.groovyserv.platform.EnvironmentVariables} class.
 */
@UnitTest
class EnvironmentVariablesSpec extends Specification {

    def setup() {
        EnvironmentVariables.setUp()
    }

    def "put() sets a specified environment variable and then it's visible for getenv(name)"() {
        given:
        assert System.getenv("ENV_TEST_1_NAME") == null

        when:
        EnvironmentVariables.instance.put("ENV_TEST_1_NAME=ENV_TEST_1_VALUE")

        then:
        System.getenv("ENV_TEST_1_NAME") == "ENV_TEST_1_VALUE"
    }

    def "put() sets a specified environment variable and then it's visible for getenv() with no argument"() {
        given:
        assert System.getenv()["ENV_TEST_2_NAME"] == null
        assert System.getenv().ENV_TEST_2_NAME == null
        int beforeEnvCount = System.getenv().size()

        when:
        EnvironmentVariables.instance.put("ENV_TEST_2_NAME=ENV_TEST_2_VALUE")

        then:
        System.getenv()["ENV_TEST_2_NAME"] == "ENV_TEST_2_VALUE"
        System.getenv().ENV_TEST_2_NAME == "ENV_TEST_2_VALUE"
        System.getenv().size() == beforeEnvCount + 1
    }

    def "put() sets a specified environment variable and then it's visible for 'env' property"() {
        given:
        assert System.env["ENV_TEST_3_NAME"] == null
        assert System.env.ENV_TEST_3_NAME == null
        int beforeEnvCount = System.env.size()

        when:
        EnvironmentVariables.instance.put("ENV_TEST_3_NAME=ENV_TEST_3_VALUE")

        then:
        System.env["ENV_TEST_3_NAME"] == "ENV_TEST_3_VALUE"
        System.env.ENV_TEST_3_NAME == "ENV_TEST_3_VALUE"
        System.env.size() == beforeEnvCount + 1
    }

}
