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
package org.jggug.kobo.groovyserv.utils

import org.jggug.kobo.groovyserv.test.UnitTest
import spock.lang.Specification

/**
 * Specifications for the {@link org.jggug.kobo.groovyserv.utils.DebugUtils} class.
 */
@UnitTest
class DebugUtilsSpec extends Specification {

    private static final SEP = System.getProperty("line.separator")

    def setup() {
        System.properties.remove("groovyserver.verbose")
    }

    def cleanup() {
        System.properties.remove("groovyserver.verbose")
    }

    def "dump() with string which size is 32"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 0, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef
              |30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46 | 0123456789ABCDEF
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 31"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDE".bytes

        expect:
        DebugUtils.dump(data, 0, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef
              |30 31 32 33 34 35 36 37 38 39 41 42 43 44 45    | 0123456789ABCDE
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 17"() {
        given:
        byte[] data = "0123456789abcdef0".bytes

        expect:
        DebugUtils.dump(data, 0, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef
              |30                                              | 0
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 16"() {
        given:
        byte[] data = "0123456789abcdef".bytes

        expect:
        DebugUtils.dump(data, 0, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 15"() {
        given:
        byte[] data = "0123456789abcde".bytes

        expect:
        DebugUtils.dump(data, 0, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30 31 32 33 34 35 36 37 38 39 61 62 63 64 65    | 0123456789abcde
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 1"() {
        given:
        byte[] data = "0".bytes

        expect:
        DebugUtils.dump(data, 0, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30                                              | 0
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 0"() {
        given:
        byte[] data = "".bytes

        expect:
        DebugUtils.dump(data, 0, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 and from offset 1"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 1, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 30 | 123456789abcdef0
              |31 32 33 34 35 36 37 38 39 41 42 43 44 45 46    | 123456789ABCDEF
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 and from offset 31"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 31, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |46                                              | F
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 and from offset 32"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 32, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 and from offset 33"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 33, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 1 and from offset 1"() {
        given:
        byte[] data = "0".bytes

        expect:
        DebugUtils.dump(data, 1, data.size()) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 but specified length 16 as 3rd argument"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 0, 16) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 | 0123456789abcdef
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 but specified length 1 as 3rd argument"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 0, 1) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30                                              | 0
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 but specified length 0 as 3rd argument"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 0, 0) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 1 but specified length 0 as 3rd argument"() {
        given:
        byte[] data = "0".bytes

        expect:
        DebugUtils.dump(data, 0, 16) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |30                                              | 0
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 from offset 8 but specified length 16 as 3rd argument"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 8, 16) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |38 39 61 62 63 64 65 66 30 31 32 33 34 35 36 37 | 89abcdef01234567
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with string which size is 32 from offset 8 but specified length 15 as 3rd argument"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        expect:
        DebugUtils.dump(data, 8, 15) ==
            '''+-----------+-----------+-----------+-----------+----------------+
              |38 39 61 62 63 64 65 66 30 31 32 33 34 35 36    | 89abcdef0123456
              |+-----------+-----------+-----------+-----------+----------------+'''.stripMargin().replaceAll(/\r?\n/, SEP)
    }

    def "dump() with negative offset value"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        when:
        DebugUtils.dump(data, -1, data.size() - 1)

        then:
        thrown IllegalArgumentException
    }

    def "dump() with negative length value"() {
        given:
        byte[] data = "0123456789abcdef0123456789ABCDEF".bytes

        when:
        DebugUtils.dump(data, 0, -1)

        then:
        thrown IllegalArgumentException
    }

    def "isVerboseMode()"() {
        given:
        if (input) System.properties["groovyserver.verbose"] = input

        expect:
        DebugUtils.isVerboseMode() == expected

        where:
        input   | expected
        "true"  | true
        "TRUE"  | true
        null    | false
        "false" | false
    }
}
