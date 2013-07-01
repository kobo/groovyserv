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

/**
 * Tests for the {@link org.jggug.kobo.groovyserv.platform.EnvironmentVariables} class.
 */
class EnvironmentVariablesTest extends GroovyTestCase {

    void setUp() {
        EnvironmentVariables.setUp()
    }

    void testPutAndGetenvWithName() {
        // Before
        assert System.getenv("ENV_TEST_1_NAME") == null
        // Exercise
        EnvironmentVariables.instance.put("ENV_TEST_1_NAME=ENV_TEST_1_VALUE")
        // Verify
        assert System.getenv("ENV_TEST_1_NAME") == "ENV_TEST_1_VALUE"
    }

    void testPutAndGetenvWithNoArg() {
        // Before
        assert System.getenv()["ENV_TEST_2_NAME"] == null
        assert System.getenv().ENV_TEST_2_NAME == null
        int beforeEnvCount = System.getenv().size()
        // Exercise
        EnvironmentVariables.instance.put("ENV_TEST_2_NAME=ENV_TEST_2_VALUE")
        // Verify
        assert System.getenv()["ENV_TEST_2_NAME"] == "ENV_TEST_2_VALUE"
        assert System.getenv().ENV_TEST_2_NAME == "ENV_TEST_2_VALUE"
        assert System.getenv().size() == beforeEnvCount + 1
    }

    void testPutAndEnvProperty() {
        // Before
        assert System.env["ENV_TEST_3_NAME"] == null
        assert System.env.ENV_TEST_3_NAME == null
        int beforeEnvCount = System.env.size()
        // Exercise
        EnvironmentVariables.instance.put("ENV_TEST_3_NAME=ENV_TEST_3_VALUE")
        // Verify
        assert System.env["ENV_TEST_3_NAME"] == "ENV_TEST_3_VALUE"
        assert System.env.ENV_TEST_3_NAME == "ENV_TEST_3_VALUE"
        assert System.env.size() == beforeEnvCount + 1
    }

}
