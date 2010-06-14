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

import static java.lang.Thread.currentThread as currentThread


/**
 * A repository of ClientConnection for each ThreadGroup.
 *
 * @author NAKANO Yasuharu
 */
@Singleton
class ClientConnectionRepository {

    private WeakHashMap<ThreadGroup, ClientConnection> connectionPerThreadGroup = [:]

    void bind(ThreadGroup tg, ClientConnection connection) {
        connectionPerThreadGroup[tg] = connection
        DebugUtils.verboseLog "ClientConnectionRepository: Client connection is bound: ${connection.socket.port} to ${tg.name}"
    }

    void unbind(ThreadGroup tg) {
        def connection = connectionPerThreadGroup.remove(tg)
        if (connection) {
            DebugUtils.verboseLog "ClientConnectionRepository: Client connection is unbound: ${connection.socket.port} from ${tg.name}"
        }
    }

    ClientConnection getCurrentConnection() {
        def thread = currentThread()
        check(findConnection(thread.threadGroup), thread)
    }

    private ClientConnection findConnection(threadGroup) {
        def conn = connectionPerThreadGroup[threadGroup]
        if (conn) {
            DebugUtils.verboseLog("ClientConnectionRepository: Found client connection: ${threadGroup}: ${conn}")
            return conn
        }
        if (threadGroup.parent == null) {
            DebugUtils.verboseLog("ClientConnectionRepository: Not found client connection: ${threadGroup}")
            return null
        }
        return findConnection(threadGroup.parent)
    }

    private static check(connection, currentThread) {
        if (connection == null) {
            throw new GroovyServerIllegalStateException("ClientConnectionRepository: This thread cannot access to standard streams: ${currentThread}")
        }
        return connection
    }

}

