#!/usr/bin/env groovy
/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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


/**
 * @author NAKANO Yasuharu
 */
public enum ExitStatus {

    SUCCESS(0),
    INTERRUPTED(-1),
    INVALID_REQUEST(-2),
    IO_ERROR(-3),
    ILLEGAL_STATE(-4),
    UNEXPECTED_ERROR(-9)

    int code

    private ExitStatus(code) {
        this.code = code
    }

}

