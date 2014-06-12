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
	"bufio"
	b64 "encoding/base64"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"net"
	"os"
	"os/exec"
	"os/signal"
	"path"
	"regexp"
	"strconv"
	"strings"
	"syscall"
)

const (
	DEFAULT_HOST             = "localhost"
	DEFAULT_PORT             = 1961
	ERROR_INVALID_AUTHTOKEN  = 201
	ERROR_CLIENT_NOT_ALLOWED = 202
	BUFFER_SIZE              = 4096
	USAGE                    = `usage: groovyclient -C[option for groovyclient] [args/options for groovy]
options:
  -Ch,-Chelp                       show this usage
  -Cs,-Chost                       specify the host to connect to groovyserver
  -Cp,-Cport <port>                specify the port to connect to groovyserver
  -Ca,-Cauthtoken <authtoken>      specify the authtoken
  -Ck,-Ckill-server                kill the running groovyserver
  -Cr,-Crestart-server             restart the running groovyserver
  -Cq,-Cquiet                      suppress statring messages
  -Cenv <substr>                   pass environment variables of which a name
                                   includes specified substr
  -Cenv-all                        pass all environment variables
  -Cenv-exclude <substr>           don't pass environment variables of which a
                                   name includes specified substr
  -Cv,-Cversion                    display the GroovyServ version
  -Cdebug                          display console log`
)

var (
	HOME_DIR            = gs.HomeDir()
	GROOVYSERV_HOME_DIR = gs.Env("GROOVYSERV_HOME", path.Join(path.Dir(os.Args[0]), ".."))
	VERSION_MESSAGE     = "GroovyServ Version: Client: " + GROOVYSERV_VERSION
	GROOVYSERV_VERSION  = "x.x" // replaced by ldflags
)

type (
	ClientOptions struct {
		host            string
		port            int
		authToken       string
		quiet           bool
		envAll          bool
		envIncludeMask  []string
		envExcludeMask  []string
		help            bool
		version         bool
		debug           bool
		startServerOpts []string
	}

	ServerOptions struct {
		help    bool
		version bool
		args    []string
	}

	Options struct {
		client ClientOptions
		server ServerOptions
	}
)

func NewOptions() Options {
	return Options{
		client: ClientOptions{
			host:            gs.Env("GROOVYSERV_HOST", DEFAULT_HOST),
			port:            gs.EnvInt("GROOVYSERV_PORT", DEFAULT_PORT),
			authToken:       "",
			quiet:           false,
			envAll:          false,
			envIncludeMask:  []string{},
			envExcludeMask:  []string{},
			help:            false,
			version:         false,
			debug:           false,
			startServerOpts: []string{},
		},
		server: ServerOptions{
			help:    false,
			version: false,
			args:    []string{},
		},
	}
}

func ParseOptions(args gs.Args) Options {
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
		case "-Ck", "-Ckill-server":
			opts.client.startServerOpts = append(opts.client.startServerOpts, "-k")
		case "-Cr", "-Crestart-server":
			opts.client.startServerOpts = append(opts.client.startServerOpts, "-r")
		default:
			if m, _ := regexp.MatchString("-C.*", arg); m {
				panic(fmt.Sprintf("unrecognized option %s", arg))
			}
			opts.server.args = append(opts.server.args, arg)
		}
	}
	// To show a help usage if no args for server.
	if len(opts.server.args) == 0 {
		opts.server.help = true
	}
	return opts
}

func HandleSignal(writer io.Writer) {
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)
	for sig := range c {
		log.Printf("Signaled: %#v", sig)
		gs.Write(writer, "Cmd: interrupt\n\n")
		os.Exit(1)
	}
}

func StartSession(opts Options) {
	// Connecting to server process.
	conn, err := gs.Connect(opts.client.host, opts.client.port)
	if err != nil {
		// If a server isn't running, try to start it once.
		if oerr, ok := err.(*net.OpError); ok && syscall.ECONNREFUSED == oerr.Err {
			StartServer(opts)
			conn, err = gs.Connect(opts.client.host, opts.client.port)
			if err != nil {
				panic(fmt.Sprintf("could not connect to server: %s", err.Error()))
			}
		} else {
			panic(fmt.Sprintf("could not connect to server: %s", err.Error()))
		}
	}
	defer conn.Close()

	go HandleSignal(conn)
	WriteInvocationRequest(conn, opts)
	go WriteStreamRequest(conn)
	ReadResponse(conn, opts)
}

func WriteStreamRequest(writer io.Writer) {
	reader := bufio.NewReader(os.Stdin)
	for {
		input, err := reader.ReadString('\n')
		if err != nil {
			gs.Write(writer, "Size: 0\n\n")
			break
		}
		gs.Write(writer, fmt.Sprintf("Size: %d\n\n", len(input)))
		gs.Write(writer, input) // without LF
	}
}

func ReadResponse(reader io.Reader, opts Options) {
	rd := bufio.NewReaderSize(reader, BUFFER_SIZE)
	buff := make([]byte, BUFFER_SIZE)
	for {
		// Reading headers
		headers := ReadHeaders(rd)
		if len(headers) == 0 {
			panic(fmt.Sprintf("no header"))
		}

		// Status:
		statusCodeStr, ok := headers.Value("Status")
		if ok {
			log.Println("Status found:", statusCodeStr)

			if opts.server.help {
				fmt.Println("")
				fmt.Println(USAGE)
			}
			if opts.server.version {
				fmt.Println(VERSION_MESSAGE)
			}

			statusCode, _ := strconv.Atoi(statusCodeStr)
			switch statusCode {
			case ERROR_INVALID_AUTHTOKEN:
				panic(fmt.Sprintf("invalid authtoken"))
			case ERROR_CLIENT_NOT_ALLOWED:
				panic(fmt.Sprintf("client address not allowed %s:%d", opts.client.host, opts.client.port))
			}

			os.Exit(statusCode)
		}

		// Size:
		bodySizeStr, ok := headers.Value("Size")
		if !ok {
			panic(fmt.Sprintf("no response: %#v", headers))
		}
		bodySize, _ := strconv.Atoi(bodySizeStr)

		// Channel:
		channel, _ := headers.Value("Channel")
		targetStream := os.Stdout
		if channel == "err" {
			targetStream = os.Stderr
		}

		// Reading body
		var err error
		for n, read := 0, 0; read < bodySize; read += n {
			n, err = rd.Read(buff[0:bodySize])
			if err == io.EOF {
				break
			}
			if err != nil {
				panic(fmt.Sprintf("could not read body from server: %s", err.Error()))
			}
			fmt.Fprint(targetStream, string(buff[0:n]))
		}
	}
}

type Headers map[string]string

func (headers Headers) Value(key string) (value string, ok bool) {
	value, ok = headers[key]
	return strings.Trim(value, " "), ok
}

func ReadHeaders(reader *bufio.Reader) Headers {
	headers := Headers{}
	for {
		line, err := gs.ReadLine(reader)
		if err != io.EOF && err != nil { // EOF isn't error
			panic(fmt.Sprintf("could not read headers from server: %s", err.Error()))
		}
		tokens := strings.SplitN(line, ":", 2)
		if len(tokens) < 2 {
			return headers
		}
		key := tokens[0]
		value := tokens[1]
		headers[key] = value
		log.Printf("Key/Value: '%s' => '%s'\n", key, value)
	}
	return headers
}

func WriteInvocationRequest(writer io.Writer, opts Options) {
	gs.WriteLine(writer, "Cwd: /tmp")
	gs.WriteLine(writer, "Auth: "+AuthToken(opts.client.port, opts))
	for _, arg := range opts.server.args {
		gs.WriteLine(writer, "Arg: "+b64.StdEncoding.EncodeToString([]byte(arg)))
	}
	WriteEnv(writer, opts)
	if cp := gs.Env("CLASSPATH", ""); cp != "" {
		gs.WriteLine(writer, "Cp: "+cp)
	}
	gs.WriteLine(writer, "")
}

func WriteEnv(writer io.Writer, opts Options) {
	for _, env := range os.Environ() {
		name := strings.SplitN(env, "=", 2)[0]
		maskMatcher := func(mask string) bool {
			return strings.Index(name, mask) >= 0
		}
		if opts.client.envAll || gs.ArrayFuncAny(opts.client.envIncludeMask, maskMatcher) {
			if !gs.ArrayFuncAny(opts.client.envExcludeMask, maskMatcher) {
				gs.WriteLine(writer, "Env: "+env)
			}
		}
	}
}

func AuthToken(port int, opts Options) string {
	// User specified token has the highest priority.
	if opts.client.authToken != "" {
		return opts.client.authToken
	}

	// Reading a stored token from a file.
	authTokenPath := path.Join(HOME_DIR, fmt.Sprintf(".groovy/groovyserv/authtoken-%d", port))
	body, err := ioutil.ReadFile(authTokenPath)
	if err != nil {
		panic(fmt.Sprintf("could not read authtoken file: %s", err.Error()))
	}
	authToken := string(body)
	log.Println("AuthToken:", authToken)
	return authToken
}

func StartServer(opts Options) {
	serverCmdFile := path.Join(GROOVYSERV_HOME_DIR, "bin/groovyserver")
	if !gs.FileExists(serverCmdFile) {
		panic(fmt.Sprintf("server command not found %s", serverCmdFile))
	}
	log.Printf("groovyserver command path: %s", serverCmdFile)

	cmd := exec.Command(serverCmdFile, "-p", fmt.Sprint(opts.client.port), strings.Join(opts.client.startServerOpts, " "))
	if !opts.client.quiet {
		fmt.Fprintf(os.Stderr, "Start server: %s -p %d %s\n", serverCmdFile, opts.client.port, strings.Join(opts.client.startServerOpts, " "))
		cmd.Stdout, cmd.Stderr = os.Stdout, os.Stderr
	}
	err := cmd.Run()
	if err != nil {
		panic(fmt.Sprintf("could not start groovyserver: %s", err.Error()))
	}
}

func main() {
	defer gs.ExitIfPanic()

	// Parsing options
	opts := ParseOptions(os.Args)

	// Setting log level
	if !opts.client.debug {
		log.SetOutput(ioutil.Discard)
	}
	log.Println("Original arguments:", os.Args)
	log.Printf("Parsed options: %#v", opts) // must be after affecting -Cdebug option

	// Only show usage (highest priority)
	if opts.client.help {
		fmt.Println(USAGE)
		os.Exit(0)
	}

	// Only show version (highest priority)
	if opts.client.version {
		fmt.Println(VERSION_MESSAGE)
		os.Exit(0)
	}

	// Start or kill server when specified
	if gs.ArrayAny(opts.client.startServerOpts, "-k", "-r") {
		StartServer(opts)
		if gs.ArrayAny(opts.client.startServerOpts, "-k") {
			os.Exit(0)
		}
	}

	// Make a groovy script evaluate on a server
	StartSession(opts)

	// Only show version (highest priority)
	if opts.server.version {
		fmt.Println(VERSION_MESSAGE)
		os.Exit(0)
	}
}
