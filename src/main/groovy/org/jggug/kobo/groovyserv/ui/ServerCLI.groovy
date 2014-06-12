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
package org.jggug.kobo.groovyserv.ui

import org.jggug.kobo.groovyserv.AuthToken
import org.jggug.kobo.groovyserv.ExitStatus
import org.jggug.kobo.groovyserv.GroovyClient
import org.jggug.kobo.groovyserv.GroovyServer
import org.jggug.kobo.groovyserv.WorkFiles
import org.jggug.kobo.groovyserv.platform.PlatformMethods
import org.jggug.kobo.groovyserv.utils.Holders
import org.jggug.kobo.groovyserv.utils.LogUtils
/**
 * Facade for script of groovyserver.*
 *
 * @author Yasuharu NAKANO
 */
class ServerCLI {

    private File serverScriptPath
    private List<String> args
    private options
    private int port
    private boolean quiet

    static void main(String[] args) {
        assert args.size() > 0: "script path is required"
        new ServerCLI(args[0], args.tail().toList()).server()
    }

    ServerCLI(String serverScriptPath, List<String> args) {
        this.serverScriptPath = new File(serverScriptPath)
        this.args = args
        this.options = parseOptions(args)
        this.port = getPortNumber(options)
        this.quiet = options.quiet
        WorkFiles.setUp(port) // for authtoken
        LogUtils.debug = options.verbose
        LogUtils.debugLog "Started by script: ${serverScriptPath}"
        LogUtils.debugLog "Server command: ${args}"
    }

    void server() {
        try {
            // If running as daemon, a server is started up directly.
            if (isDaemonized()) {
                startServerDirectlyAndWaitFor() // infinite blocking operation
                return
            }

            // Handling commands.
            if (options.restart) {
                shutdownServer()
            }
            if (options.kill) {
                shutdownServer()
                return
            }
            startServerAsDaemon()
        }
        catch (Throwable e) {
            LogUtils.errorLog "Failed to server a server command", e
            die "ERROR: unexpected error: ${e.message}"
        }
    }

    private void shutdownServer() {
        def client = newGroovyClient()

        // For usability, the following code checks status in detail and shows an appropriate message.

        // If server is NOT already running, it needs to do nothing.
        if (!client.canConnect()) {
            println "WARN: server is not running"
            if (WorkFiles.AUTHTOKEN_FILE.exists()) {
                println "WARN: old authtoken file is deleted: ${WorkFiles.AUTHTOKEN_FILE}"
                WorkFiles.AUTHTOKEN_FILE.delete()
            }
            return
        }
        LogUtils.debugLog "before makeSureServerAvailableOrDie"
        makeSureServerAvailableOrDie(client)

        // From here, the process of the port is really GroovyServ's process and still alive.

        // Shutting down
        print "Shutting down server..."
        try {
            def exitStatus = client.connect().shutdown().waitForExit().exitStatus
            if (exitStatus != ExitStatus.SUCCESS.code) {
                LogUtils.errorLog "Failed to kill the server: ${exitStatus}"
                println "" // clear for print
                die "ERROR: could not kill server: ${exitStatus}", // cannot test
                    "Hint:  Make sure the server process is still alive. If so, kill the process somehow."
            }
        }
        catch (Throwable e) {
            LogUtils.errorLog "Failed to kill the server", e
            println "" // clear for print
            die "ERROR: could not kill server: ${e.message}", // cannot test
                "Hint:  Make sure the server process is still alive. If so, kill the process somehow."
        }

        // Waiting for server down
        while (WorkFiles.AUTHTOKEN_FILE.exists() || !client.isServerShutdown()) {
            print "."
            sleep 200
        }
        println "" // clear for print
        println "Server is successfully shut down"
    }

    private void makeSureServerAvailableOrDie(GroovyClient client) {
        // If there is no authtoken, to access server is impossible.
        if (!WorkFiles.AUTHTOKEN_FILE.exists()) {
            die "ERROR: could not open authtoken file: ${WorkFiles.AUTHTOKEN_FILE}",
                "Hint:  Isn't the port being used by a non-groovyserv process? Use another port or kill the process somehow."
        }
        if (!WorkFiles.AUTHTOKEN_FILE.isFile() || !WorkFiles.AUTHTOKEN_FILE.canRead()) {
            die "ERROR: could not read authtoken file: ${WorkFiles.AUTHTOKEN_FILE}",
                "Hint:  Check the permission and file type of ${WorkFiles.AUTHTOKEN_FILE}."
        }
        // Is server really available?
        if (!client.isServerAvailable()) {
            die "ERROR: could not access server", // cannot test
                "Hint:  Isn't the port being used by a non-groovyserv process? Use another port or kill the process somehow."
        }
        if (client.exitStatus == ExitStatus.INVALID_AUTHTOKEN.code) {
            die "ERROR: invalid authtoken",
                "Hint:  Specify a right authtoken or kill the process somehow."
        }
        if (client.exitStatus != ExitStatus.SUCCESS.code) { // cannot test
            die "ERROR: server exiting abnormally: ${client.exitStatus}"
        }
    }

    private void startServerAsDaemon() {
        def client = newGroovyClient()

        // For usability, the following code checks status in detail and shows an appropriate message.

        // If server is already running, it needs to do nothing.
        if (client.canConnect()) {
            makeSureServerAvailableOrDie(client)
            println "WARN: server is already running on ${port} port"
            return
        }

        // If there is authtoken file, it must be old and unnecessary.
        if (WorkFiles.AUTHTOKEN_FILE.exists()) {
            println "WARN: old authtoken file is deleted: ${WorkFiles.AUTHTOKEN_FILE}"
            WorkFiles.AUTHTOKEN_FILE.delete()
        }

        // From here, the port is unused by any process.

        // Starting
        print "Starting server..."
        daemonize() // start daemon process

        // Waiting for server up
        sleep 2000
        while (!WorkFiles.AUTHTOKEN_FILE.exists() || !client.isServerAvailable()) {
            print "."
            sleep 200
        }
        println "" // clear for print
        println "Server is successfully started up on $port port"
    }

    private void startServerDirectlyAndWaitFor() {
        if (Holders.groovyServer) {
            die "ERROR: GroovyServer is already started in the same JVM"
        }

        // Setup GroovyServer instance
        def groovyServer = new GroovyServer()
        groovyServer.port = port
        if (options.authtoken) groovyServer.authToken = new AuthToken(options.authtoken)
        if (options."allow-from") groovyServer.allowFrom = options["allow-from"]?.split(',')

        // Set holders for global access
        // This is necessary for RequestWorker's call of shutdown.
        Holders.groovyServer = groovyServer

        // Start server (blocking operation)
        groovyServer.start()
    }

    private GroovyClient newGroovyClient() {
        return new GroovyClient(host: "localhost", port: port)
    }

    private parseOptions(args) {
        def cli = new CliBuilder(usage: "${serverScriptPath.name} [options]")
        cli.with {
            h longOpt: 'help', 'show this usage'
            v longOpt: 'verbose', "verbose output to a log file"
            q longOpt: 'quiet', "suppress output to console except error message"
            k longOpt: 'kill', "kill the running groovyserver"
            r longOpt: 'restart', "restart the running groovyserver"
            p longOpt: 'port', args: 1, argName: 'port', "specify the port to listen"
            _ longOpt: 'allow-from', args: 1, argName: 'addresses', "specify optional acceptable client addresses (delimiter: comma)"
            _ longOpt: 'authtoken', args: 1, argName: 'authtoken', "specify authtoken (which is automatically generated if not specified)"
            _ longOpt: 'daemonized', "run a groovyserver as daemon (INTERNAL USE ONLY)"
        }
        def opt = cli.parse(args)
        if (!opt) die "ERROR: could not parse arguments: ${args.join(' ')}"
        assert !opt.help: "help usage should be shown by a front script"
        if (opt.kill && opt.restart) {
            die "ERROR: invalid arguments: ${args.join(' ')}",
                "Hint: You cannot specify --kill and --restart options at the same time."
        }
        return opt
    }

    private boolean isDaemonized() {
        return options.daemonized
    }

    private void daemonize() {
        def command = convertExecutableCommand([serverScriptPath.absolutePath, "--daemonized", "-q", * args])
        LogUtils.debugLog "Daemonizing by restarting a server script: $command"
        command.execute()
    }

    private static List<String> convertExecutableCommand(List<String> command) {
        if (PlatformMethods.isWindows()) {
            return ["cmd.exe", "/c"] + command
        }
        return command
    }

    private println(message) {
        if (!quiet) System.err.println message
    }

    private print(message) {
        if (!quiet) System.err.print message
    }

    private static die(message, hint = null) {
        LogUtils.errorLog "Died with message: ${message}${hint ? ", ${hint}" : ""}"
        System.err.println message
        if (hint) System.err.println hint
        exit ExitStatus.COMMAND_ERROR
    }

    private static exit(ExitStatus status) {
        System.setSecurityManager(null)
        System.exit(status.code)
    }

    private static int getPortNumber(options) {
        return (options.port ?: GroovyServer.DEFAULT_PORT) as int
    }
}
