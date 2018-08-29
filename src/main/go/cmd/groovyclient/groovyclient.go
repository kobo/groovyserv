/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, version 2.0 (the "License");
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
package main

import (
	cmn "../../common"
	srv "../../server"
	"fmt"
	"log"
	"os"
	"regexp"
	"strconv"
	"strings"
)

const (
	USAGE = `usage: groovyclient -C[option for groovyclient] [args/options for groovy]
options:
  -Ch,-Chelp                       show this usage
  -Cs,-Chost                       specify host to connect to server
  -Cp,-Cport <port>                specify port to connect to server
  -Ca,-Cauthtoken <authtoken>      specify authtoken
  -Ck,-Ckill-server                kill the running server
  -Cr,-Crestart-server             restart the running server
  -Ct,-Ctimeout <second>           specify a timeout waiting for starting up a
                                   server process (default: 20 sec)
  -Cq,-Cquiet                      suppress starting messages
  -Cenv <substr>                   pass environment variables of which a name
                                   includes specified substr
  -Cenv-all                        pass all environment variables
  -Cenv-exclude <substr>           don't pass environment variables of which a
                                   name includes specified substr
  -Cv,-Cversion                    display the version
  -Ckeep-server-cwd                avoid to change directory to current working
                                   directory of client
  -Cdebug                          display console log`
)

var (
	VersionMessage    = "GroovyServ Version: Client: " + GroovyServVersion
	GroovyServVersion = "X.X.X-SNAPSHOT" // replaced by ldflags
)

type (
	ClientOptions struct {
		host           string
		port           int
		authToken      string
		quiet          bool
		envAll         bool
		envIncludeMask []string
		envExcludeMask []string
		help           bool
		version        bool
		debug          bool
		keepServerCwd  bool
	}

	ServerOptions struct {
		help    bool
		version bool
		args    []string
		kill    bool
		restart bool
		timeout int
	}

	Options struct {
		client ClientOptions
		server ServerOptions
	}
)

func NewOptions() *Options {
	return &Options{
		client: ClientOptions{
			host:           cmn.Env("GROOVYSERV_HOST", srv.DefaultHost),
			port:           cmn.EnvInt("GROOVYSERV_PORT", srv.DefaultPort),
			authToken:      "",
			quiet:          false,
			envAll:         false,
			envIncludeMask: []string{},
			envExcludeMask: []string{},
			help:           false,
			version:        false,
			debug:          false,
			keepServerCwd:  false,
		},
		server: ServerOptions{
			help:    false,
			version: false,
			args:    []string{},
			kill:    false,
			restart: false,
			timeout: srv.DefaultTimeout,
		},
	}
}

func ParseOptions(args cmn.Args) *Options {
	opts := NewOptions()
	for i := 1; i < len(args); i++ {
		arg, param := args[i], ""

		// Replacing "-v.*" -> "--version". This behavior is as same as regular groovy command.
		if regexp.MustCompile(`^-v.*`).MatchString(arg) {
			arg = "-v"
		}

		switch arg {
		case "-Cs", "-Chost":
			i, param = args.Param(i, arg)
			opts.client.host = param
		case "-Cp", "-Cport":
			i, param = args.Param(i, arg)
			port, err := strconv.Atoi(param)
			if err != nil {
				panic(fmt.Sprintf("could not parse port number '%s'", param))
			}
			opts.client.port = port
		case "-Ca", "-Cauthtoken":
			i, param = args.Param(i, arg)
			opts.client.authToken = param
		case "-Cq", "-Cquiet":
			opts.client.quiet = true
		case "-Cenv-all":
			opts.client.envAll = true
		case "-Cenv", "-Cenv-exclude":
			i, param = args.Param(i, arg)
			if arg == "-Cenv" {
				opts.client.envIncludeMask = append(opts.client.envIncludeMask, param)
			} else {
				opts.client.envExcludeMask = append(opts.client.envExcludeMask, param)
			}
		case "-Ch", "-Chelp":
			opts.client.help = true
		case "-h", "--help", "-help":
			opts.server.help = true
			opts.server.args = append(opts.server.args, arg)
		case "-Cv", "-Cversion":
			opts.client.version = true
		case "--version", "-v":
			opts.server.version = true
			opts.server.args = append(opts.server.args, arg)
		case "-Cdebug":
			opts.client.debug = true
		case "-Ckeep-server-cwd":
			opts.client.keepServerCwd = true
		case "-Ck", "-Ckill-server":
			opts.server.kill = true
		case "-Cr", "-Crestart-server":
			opts.server.restart = true
		case "-Ct", "-Ctimeout":
			i, param = args.Param(i, arg)
			timeout, err := strconv.Atoi(param)
			if err != nil {
				panic(fmt.Sprintf("could not parse timeout '%s'", param))
			}
			opts.server.timeout = timeout * 1000
		default:
			if m, _ := regexp.MatchString("^-C.*", arg); m {
				panic(fmt.Sprintf("unrecognized option %s", arg))
			}
			opts.server.args = append(opts.server.args, arg)
		}
	}
	if opts.server.kill && opts.server.restart {
		panic(fmt.Sprintf("invalid arguments: %s\nHint: You cannot specify -Ckill-server and -Crestart-server options at the same time.", strings.Join(args[1:], " ")))
	}
	// To show a help usage if no args for server.
	if len(opts.server.args) == 0 {
		opts.server.help = true
	}
	return opts
}

func main() {
	defer cmn.ExitIfPanic()

	// Parsing options
	opts := ParseOptions(os.Args)

	// Setting log level
	cmn.SetupGlobalLogger(!opts.client.debug)
	log.Println("Original arguments:", os.Args)
	log.Printf("Parsed options: %#v", opts) // must be after affecting -Cdebug option

	// Only show usage or version (highest priority)
	switch {
	case opts.client.help:
		fmt.Println(USAGE)
		os.Exit(0)
	case opts.client.version:
		fmt.Println(VersionMessage)
		os.Exit(0)
	}

	// Setting up a server for running a process
	var server = srv.Server{
		Args:           opts.server.args,
		Host:           opts.client.host,
		Port:           opts.client.port,
		AllowFrom:      "",
		AuthToken:      opts.client.authToken,
		Quiet:          opts.client.quiet,
		EnvAll:         opts.client.envAll,
		EnvIncludeMask: opts.client.envIncludeMask,
		EnvExcludeMask: opts.client.envExcludeMask,
		Help:           opts.client.help,
		Version:        opts.client.version,
		Verbose:        false,
		Debug:          opts.client.debug,
		KeepServerCwd:  opts.client.keepServerCwd,
	}

	// Shutting down a server when specified
	if opts.server.kill || opts.server.restart {
		if err := server.Shutdown(); err != nil {
			panic(err.Error())
		}
		if opts.server.kill {
			os.Exit(0)
		}
	}

	// Starting a server if not running
	if server.Dead() {
		if err := server.Start(opts.server.timeout); err != nil {
			panic(err.Error())
		}
	}

	// Make a groovy script evaluate on a server
	statusCode, err := server.RunScript()
	if err != nil {
		panic(err.Error())
	}

	// Only show usage or version (highest priority)
	switch {
	case opts.server.help:
		fmt.Println("")
		fmt.Println(USAGE)
	case opts.server.version:
		fmt.Println(VersionMessage)
	}

	os.Exit(statusCode)
}
