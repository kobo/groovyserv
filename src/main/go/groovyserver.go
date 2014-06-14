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
	gs "./common"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"path/filepath"
)

const (
	DEFAULT_HOST = "localhost"
	DEFAULT_PORT = 1961
	BUFFER_SIZE  = 4096
	USAGE        = `usage: groovyserver [options]
options:
  -h,--help                     show this usage
  -k,--kill                     kill the running groovyserver
  -p,--port <port>              specify the port to listen
  -r,--restart                  restart the running groovyserver
  -q,--quiet                    suppress statring messages
  -v,--verbose                  verbose output to a log file
     --allow-from <addresses>   specify optional acceptable client addresses (delimiter: comma)
     --authtoken <authtoken>    specify authtoken (which is automatically generated if not specified)
     --debug                    display console log`
)

var (
	HOME                = gs.HomeDir()
	GROOVYSERV_WORK_DIR = gs.GroovyServWorkDir()
	GROOVYSERV_HOME     = gs.GroovyServHome(os.Args[0])
	GROOVY_HOME         = gs.Env("GROOVY_HOME", "")
	GROOVYSERV_OPTS     = gs.Env("GROOVYSERV_OPTS", "")
)

type (
	Options struct {
		quiet           bool
		help            bool
		debug           bool
		passThroughArgs []string
	}
)

func NewOptions() Options {
	return Options{
		quiet:           false,
		help:            false,
		debug:           false,
		passThroughArgs: []string{},
	}
}

func ParseOptions(args gs.Args) Options {
	opts := NewOptions()
	for i := 1; i < len(args); i++ {
		arg := args[i]
		switch arg {
		case "-q", "--quiet":
			opts.quiet = true
		case "-h", "--help":
			opts.help = true
		case "--debug":
			opts.debug = true
		default:
			opts.passThroughArgs = append(opts.passThroughArgs, arg)
		}
	}
	return opts
}

func GroovyCommand(opts Options) string {
	groovyCmdName := "groovy"
	if gs.Windows() {
		groovyCmdName += ".bat"
	}
	if len(GROOVY_HOME) > 0 {
		groovyCmdPath := filepath.Join(GROOVY_HOME, "bin", groovyCmdName)
		if !gs.FileExists(groovyCmdPath) {
			panic("invalid GROOVY_HOME: " + GROOVY_HOME)
		}
		printConsole("Groovy command path: "+groovyCmdPath+" (found at GROOVY_HOME)", opts)
		return groovyCmdPath
	}
	if gs.CommandExists(groovyCmdName) {
		printConsole("Groovy command path: "+groovyCmdName+" (found at PATH)", opts)
		return groovyCmdName
	}
	panic("groovy command not found\nHint:  Requires either PATH having groovy command or GROOVY_HOME.")
}

func StartServer(opts Options) {
	printConsole("Groovy home directory: "+GROOVY_HOME, opts)
	groovyCmdPath := GroovyCommand(opts)
	printConsole("GroovyServ home directory: "+GROOVYSERV_HOME, opts)
	printConsole("GroovyServ work directory: "+GROOVYSERV_WORK_DIR, opts)

	// Setting CLASS_PATH
	classpath := filepath.Join(GROOVYSERV_HOME, "lib", "*")
	classpathEnv := os.Getenv("CLASSPATH")
	if len(classpathEnv) > 0 {
		classpath = classpath+gs.ClasspathDelimiter()+classpathEnv
	}
	printConsole("Original classpath: "+gs.Coalesce(classpathEnv, "(none)"), opts)
	printConsole("GroovyServ default classpath: "+classpath, opts)

	// Setting JAVA_OPTS
	// -server: for performance (experimental)
	// -Djava.awt.headless=true: without this, annoying to switch an active process to it when new process is created as daemon
	javaOpts := "-server -Djava.awt.headless=true " + gs.Env("JAVA_OPTS", "")

	// Preparing a command
	groovyServerCmdPath := os.Args[0]
	cmd := exec.Command(groovyCmdPath)
	if len(GROOVYSERV_OPTS) > 0 {
		cmd.Args = append(cmd.Args, GROOVYSERV_OPTS)
	}
	cmd.Args = append(cmd.Args, "-e", "org.jggug.kobo.groovyserv.ui.ServerCLI.main(args)", "--", groovyServerCmdPath)
	for _, arg := range opts.passThroughArgs {
		cmd.Args = append(cmd.Args, arg)
	}
	cmd.Env = append(os.Environ(), "CLASSPATH="+classpath, "JAVAOPTS="+javaOpts)
	if !opts.quiet {
		cmd.Stdout, cmd.Stderr = os.Stdout, os.Stderr
	}
	log.Printf("Command: %#v", cmd)

	// Running the command
	if err := cmd.Run(); err != nil {
		panic(fmt.Sprintf("could not start groovyserver: %s", err.Error()))
	}
}

func printConsole(message string, opts Options) {
	if !opts.quiet {
		fmt.Fprintln(os.Stderr, message)
	}
}

func main() {
	defer gs.ExitIfPanic()

	// Parsing options
	opts := ParseOptions(os.Args)

	// Setting log level
	if !opts.debug {
		log.SetOutput(ioutil.Discard)
	}
	log.Println("Original arguments:", os.Args)
	log.Printf("Parsed options: %#v", opts) // must be after affecting -Cdebug option

	// Only show usage (highest priority)
	if opts.help {
		fmt.Println(USAGE)
		os.Exit(0)
	}

	StartServer(opts)
}

// TODO windows support
