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
	"strconv"
	"strings"
)

const (
	USAGE = `usage: groovyserver [options]
options:
  -h,--help                        show this usage
  -k,--kill                        kill the running server
  -r,--restart                     restart the running server
  -t,--timeout <second>            specify a timeout waiting for starting up a server process (default: 20 sec)
  -q,--quiet                       suppress statring messages
  -v,--verbose                     verbose output to a log file
  -p,--port <port>                 specify port to listen
     --allow-from <addresses>      specify optional acceptable client addresses (delimiter: comma)
     --authtoken <authtoken>       specify authtoken (which is automatically generated if not specified)
     --debug                       display console log`
)

type Options struct {
	help            bool
	kill            bool
	restart         bool
	timeout         int
	quiet           bool
	verbose         bool
	port            int
	allowFrom       string
	authToken       string
	debug           bool
	passThroughArgs []string
}

func NewOptions() *Options {
	return &Options{
		help:            false,
		kill:            false,
		restart:         false,
		timeout:         srv.DefaultTimeout,
		quiet:           false,
		verbose:         false,
		port:            cmn.EnvInt("GROOVYSERV_PORT", srv.DefaultPort),
		allowFrom:       "",
		authToken:       "",
		debug:           false,
		passThroughArgs: []string{},
	}
}

func ParseOptions(args cmn.Args) *Options {
	opts := NewOptions()
	for i := 1; i < len(args); i++ {
		arg, param := args[i], ""
		switch arg {
		case "-h", "--help":
			opts.help = true
		case "-k", "--kill":
			opts.kill = true
		case "-r", "--restart":
			opts.restart = true
		case "-t", "--timeout":
			i, param = args.Param(i, arg)
			timeout, err := strconv.Atoi(param)
			if err != nil {
				panic(fmt.Sprintf("could not parse timeout '%s'", param))
			}
			opts.timeout = timeout * 1000
		case "-q", "--quiet":
			opts.quiet = true
		case "-v", "--verbose":
			opts.verbose = true
			opts.passThroughArgs = append(opts.passThroughArgs, arg)
		case "-p", "--port":
			i, param = args.Param(i, arg)
			port, err := strconv.Atoi(param)
			if err != nil {
				panic(fmt.Sprintf("could not parse port number '%s'", param))
			}
			opts.port = port
		case "--allow-from":
			i, param = args.Param(i, arg)
			opts.allowFrom = param
		case "-a", "--authtoken":
			i, param = args.Param(i, arg)
			opts.authToken = param
		case "--debug":
			opts.debug = true
		default:
			panic(fmt.Sprintf("unrecognized option %s", arg))
		}
	}
	if opts.kill && opts.restart {
		panic(fmt.Sprintf("invalid arguments: %s\nHint: You cannot specify --kill and --restart options at the same time.", strings.Join(args[1:], " ")))
	}
	return opts
}

func main() {
	defer cmn.ExitIfPanic()

	// Parsing options
	opts := ParseOptions(os.Args)

	// Setting log level
	cmn.SetupGlobalLogger(!opts.debug)
	log.Println("Original arguments:", os.Args)
	log.Printf("Parsed options: %#v", opts) // must be after affecting -Cdebug option

	// Only show usage (highest priority)
	if opts.help {
		fmt.Println(USAGE)
		os.Exit(0)
	}

	// Setting up a server for running a process
	var server = srv.Server{
		Args:           opts.passThroughArgs,
		Host:           srv.DefaultHost, // allow to start a server only at localhost
		Port:           opts.port,
		AllowFrom:      opts.allowFrom,
		AuthToken:      opts.authToken,
		Quiet:          opts.quiet,
		EnvAll:         false,
		EnvIncludeMask: []string{},
		EnvExcludeMask: []string{},
		Help:           false,
		Version:        false,
		Verbose:        opts.verbose,
		Debug:          opts.debug,
	}

	// Handling commands.
	if opts.restart || opts.kill {
		if err := server.Shutdown(); err != nil {
			panic(err.Error())
		}
		if opts.kill {
			os.Exit(0)
		}
	}
	if err := server.Start(opts.timeout); err != nil {
		panic(err.Error())
	}
}
