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
package server

import (
	cmn "../common"
	"bufio"
	b64 "encoding/base64"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/signal"
	"strconv"
	"strings"
)

const (
	StatusSuccess         = 0
	ErrorInvalidAuthToken = 201
	ErrorClientNotAllowed = 202
	MaxBufferSize         = 4096
)

type ConnectedServer struct {
	Server
	conn   net.Conn // only connected server has a connection
	closed bool
}

func (server ConnectedServer) handleSignal() {
	log.Println("handleSignal: begin")
	defer log.Println("handleSignal: end")

	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)
	for sig := range c {
		log.Printf("Signaled: %#v", sig)
		server.Interrupt()
		os.Exit(1)
	}
}

func (server ConnectedServer) RunScript() (statusCode int, err error) {
	log.Println("RunScript: begin")
	defer func() {
		log.Println("RunScript: end:", statusCode, err)
	}()

	if server.closed {
		return -1, fmt.Errorf("connection is already closed")
	}
	go server.handleSignal()
	if err = server.writeInvocationRequest(); err != nil {
		return -1, err
	}
	go server.writeStreamRequest()
	return server.readResponse()
}

func (server ConnectedServer) runCommand(command string) (statusCode int, err error) {
	log.Println("runCommand begin:", command)
	defer func() {
		log.Println("runCommand: end:", statusCode, err)
	}()

	if server.closed {
		return -1, fmt.Errorf("connection is already closed")
	}
	server.writeCommandRequest(command)
	statusCode, err = server.readResponse()
	if _, ok := err.(*InvalidAuthTokenError); ok {
		return statusCode, err
	}
	if err != nil {
		return statusCode, fmt.Errorf("could not run a command %s: %s", command, err)
	}
	if statusCode != StatusSuccess {
		return statusCode, fmt.Errorf("command not succeed %d", statusCode)
	}
	return statusCode, nil
}

func (server ConnectedServer) Interrupt() (statusCode int, err error) {
	log.Println("Interrupt: begin")
	defer func() {
		log.Println("Interrupt: end:", statusCode, err)
	}()
	return server.runCommand("interrupt")
}

func (server ConnectedServer) Shutdown() (statusCode int, err error) {
	log.Println("Shutdown: begin")
	defer func() {
		log.Println("Shutdown: end:", statusCode, err)
	}()
	return server.runCommand("shutdown")
}

func (server ConnectedServer) Ping() (statusCode int, err error) {
	log.Println("Ping: begin")
	defer func() {
		log.Println("Ping: end:", statusCode, err)
	}()
	return server.runCommand("ping")
}

func (server *ConnectedServer) Close() (err error) {
	log.Println("Close: begin")
	defer func() {
		log.Println("Close: end:", err)
	}()

	if server.closed {
		log.Println("connection is already closed")
		return nil
	}
	if err := server.conn.Close(); err != nil {
		return fmt.Errorf("could not close a connection to server: %s", err.Error())
	}
	server.closed = true
	return nil
}

func (server ConnectedServer) writeStreamRequest() {
	log.Println("writeStreamRequest: begin")
	defer log.Println("writeStreamRequest: end")

	reader := bufio.NewReaderSize(os.Stdin, MaxBufferSize)
	for {
		input, err := reader.ReadString('\n')
		if err != nil {
			server.write("Size: 0\n\n")
			break
		}
		if err := server.write(fmt.Sprintf("Size: %d\n\n", len(input))); err != nil {
			log.Println("Failed to write Size header:", err)
		}
		if err := server.write(input); err != nil { // without LF
			log.Println("Failed to write Stdin:", input, err)
		}
	}
}

func (server ConnectedServer) readResponse() (statusCode int, err error) {
	log.Println("readResponse: begin")
	defer func() {
		log.Println("readResponse: end:", statusCode, err)
	}()

	reader := bufio.NewReaderSize(server.conn, MaxBufferSize)
	for {
		// Reading headers
		headers, err := server.readHeaders(reader)
		if err != nil {
			return -1, err
		}
		if len(headers) == 0 {
			return -1, fmt.Errorf("no header")
		}
		log.Println("Headers:", headers)

		// Status:
		statusCodeStr, ok := headers.Value("Status")
		if ok {
			log.Println("Status found:", statusCodeStr)
			statusCode, _ := strconv.Atoi(statusCodeStr)
			switch statusCode {
			case InvalidAuthTokenErrorCode:
				return -1, NewInvalidAuthTokenError()
			case ClientNotAllowedErrorCode:
				return -1, NewClientNotAllowedError(server.Host, server.Port)
			}
			return statusCode, nil
		}

		// Size:
		bodySizeStr, ok := headers.Value("Size")
		if !ok {
			return -1, fmt.Errorf("no response: %#v", headers)
		}
		bodySize, _ := strconv.Atoi(bodySizeStr)

		// Channel:
		channel, _ := headers.Value("Channel")
		targetStream := os.Stdout
		if channel == "err" {
			targetStream = os.Stderr
		}

		// Reading body
		for n, leftSize := 0, bodySize; leftSize > 0; leftSize -= n {
			buffSize := MaxBufferSize
			if leftSize < MaxBufferSize {
				buffSize = leftSize
			}
			buff := make([]byte, buffSize)
			n, err = reader.Read(buff[0:buffSize])

			if err != io.EOF && err != nil {
				return -1, fmt.Errorf("could not read body from server: %s", err.Error())
			}
			fmt.Fprint(targetStream, string(buff[0:n]))
			if n == 0 {
				break // continuing outer for loop
			}
			if err == io.EOF {
				return 0, nil
			}
		}
	}
}

func (server ConnectedServer) readHeaders(reader *bufio.Reader) (headers Headers, err error) {
	log.Println("readHeaders: begin")
	defer func() {
		log.Println("readHeaders: end:", headers, err)
	}()

	headers = Headers{}
	for {
		line, err := cmn.ReadLine(reader)
		if err != io.EOF && err != nil { // EOF isn't error
			return nil, fmt.Errorf("could not read headers from server: %s", err.Error())
		}
		tokens := strings.SplitN(line, ":", 2)
		if len(tokens) < 2 {
			return headers, nil
		}
		key := tokens[0]
		value := tokens[1]
		headers[key] = value
		log.Printf("Key/Value: '%s' => '%s'\n", key, value)
	}
	return headers, nil
}

func (server ConnectedServer) writeInvocationRequest() (err error) {
	log.Println("writeInvocationRequest: begin")
	defer func() {
		log.Println("writeInvocationRequest: end:", err)
	}()

	if err := server.writeLine("Cwd: " + cmn.Env("PWD", ".")); err != nil {
		return err
	}
	if authToken, err := server.authToken(); err != nil {
		return err
	} else {
		if err = server.writeLine("Auth: "+authToken); err != nil {
			return err
		}
	}
	for _, arg := range server.Args {
		if err := server.writeLine("Arg: " + b64.StdEncoding.EncodeToString([]byte(arg))); err != nil {
			return err
		}
	}
	if err := server.writeEnv(); err != nil {
		return err
	}
	if cp := cmn.Env("CLASSPATH", ""); cp != "" {
		if err := server.writeLine("Cp: " + cp); err != nil {
			return err
		}
	}
	if err := server.writeLine(""); err != nil {
		return err
	}
	return nil
}

func (server ConnectedServer) writeCommandRequest(command string) (err error) {
	log.Println("writeCommandRequest: begin")
	defer func() {
		log.Println("writeCommandRequest: end:", err)
	}()

	if authToken, err := server.authToken(); err != nil {
		return err
	} else {
		if err := server.writeLine("Auth: " + authToken); err != nil {
			return err
		}
	}
	if err := server.writeLine("Cmd: " + command); err != nil {
		return err
	}
	if err := server.writeLine(""); err != nil {
		return err
	}
	return nil
}

func (server ConnectedServer) writeEnv() (err error) {
	log.Println("writeEnv: begin")
	defer func() {
		log.Println("writeEnv: end:", err)
	}()

	for _, env := range os.Environ() {
		name := strings.SplitN(env, "=", 2)[0]
		maskMatcher := func(mask string) bool {
			return strings.Index(name, mask) >= 0
		}
		if server.EnvAll || cmn.ArrayFuncAny(server.EnvIncludeMask, maskMatcher) {
			if !cmn.ArrayFuncAny(server.EnvExcludeMask, maskMatcher) {
				err := server.writeLine("Env: " + env)
				if err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func (server ConnectedServer) writeLine(line string) error {
	return cmn.WriteLine(server.conn, line)
}

func (server ConnectedServer) write(line string) error {
	return cmn.Write(server.conn, line)
}
