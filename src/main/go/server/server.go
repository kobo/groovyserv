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
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

const (
	DefaultHost = "localhost"
	DefaultPort = 1961
)

var (
	GroovyHome        = cmn.Env("GROOVY_HOME", "")
	GroovyServWorkDir = groovyServWorkDir()
)

type Server struct {
	Args           []string
	Host           string
	Port           int
	AllowFrom      string
	AuthToken      string
	Quiet          bool
	EnvAll         bool
	EnvIncludeMask []string
	EnvExcludeMask []string
	Help           bool
	Version        bool
	Verbose        bool
	Debug          bool
}

func (server Server) RunScript() (statusCode int, err error) {
	log.Println("RunScript: begin")
	defer func() {
		log.Println("RunScript: end:", statusCode, err)
	}()

	connected, err := server.connect()
	if err != nil {
		return -1, err
	}
	defer connected.Close()
	statusCode, err = connected.RunScript()
	return
}

func (server Server) canConnect() (ok bool) {
	log.Println("canConnect: begin")
	defer func() {
		log.Println("canConnect: end:", ok)
	}()

	connected, err := server.connect()
	if err != nil {
		log.Printf("canConnect: could not connect to server: %s", err.Error())
		return false
	}
	if err = connected.Close(); err != nil {
		log.Printf("canConnect: connected but could not close: %s", err.Error())
		return false
	}
	return true
}

func (server Server) makeSureServerAvailable() (err error) {
	log.Println("makeSureServerAvailable: begin")
	defer func() {
		log.Println("makeSureServerAvailable: end:", err)
	}()

	// If there is no authtoken, it's impossible to access server.
	if !server.authTokenExists() {
		// It's already checked to be able to connect to server before this function is called.
		return NewHintError("could not open authtoken file: "+server.authTokenFile(), "Isn't the port being used by a non-groovyserv process? Use another port or kill the process somehow.")
	}

	// If authtoken file is invalid, it's impossible to access server.
	if _, err := server.authToken(); err != nil {
		return NewHintError("could not read authtoken file: "+server.authTokenFile(), "Check the permission and file type.")
	}

	// Is server really alive?
	if _, err := server.Alive(); err != nil {
		if _, ok := err.(*InvalidAuthTokenError); ok {
			return NewHintError("invalid authtoken", "Specify a right authtoken or kill the process somehow.")
		}
		return fmt.Errorf("server exiting abnormally: %s", err.Error())
	}
	return nil
}

func (server Server) Alive() (alive bool, err error) {
	log.Println("Alive: begin")
	defer func() {
		log.Println("Alive: end:", alive, err)
	}()

	// Sending a ping
	connected, err := server.connect()
	if err != nil {
		log.Println("Alive: could not connect to server:", err.Error())
		return false, err
	}
	defer connected.Close()
	if statusCode, err := connected.Ping(); err != nil {
		log.Println("Alive: could not ping normarlly:", statusCode, err.Error())
		return false, err
	}
	return true, nil
}

func (server Server) Dead() (dead bool) {
	log.Println("Dead: begin")
	defer func() {
		log.Println("Dead: end:", dead)
	}()

	if server.canConnect() {
		return false
	}
	return true
}

func (server Server) Shutdown() (err error) {
	log.Println("Shutdown: begin")
	defer func() {
		log.Println("Shutdown: end:", err)
	}()

	// For user, the following code checks a status in detail and shows an appropriate message.

	// If server is NOT already running, it needs to do nothing.
	if ok := server.canConnect(); !ok {
		server.printlnConsole("WARN: server is not running")
		server.deleteAuthtokenIfExists()
		return nil
	}
	log.Println("Shutdown: before makeSureServerAvailable")
	if err := server.makeSureServerAvailable(); err != nil {
		return err
	}

	// From here, it has made sure that the process of the port is really server process and still alive.

	// Shutting down
	server.printConsole("Shutting down server...")
	connected, err := server.connect()
	if err != nil {
		server.printlnConsole("") // clear for print
		log.Println("Shutdown: could not connect to server:", err.Error())
		return err
	}
	defer connected.Close()
	if _, err := connected.Shutdown(); err != nil {
		log.Println("Shutdown: could not kill server:", err.Error())
		server.printlnConsole("") // clear for print
		return NewHintError("could not kill server", "Make sure the server process is still alive. If so, kill the process somehow.")
	}

	// Waiting for server up
	for {
		server.printConsole(".")
		time.Sleep(200 * time.Millisecond)
		if !server.authTokenExists() && server.Dead() {
			break
		}
	}
	server.printlnConsole("") // clear for print
	server.printlnConsole("Server is successfully shut down")
	return nil
}

func (server Server) Start() (err error) {
	log.Println("Start: begin")
	defer func() {
		log.Println("Start: end:", err)
	}()

	// For user, the following code checks a status in detail and shows an appropriate message.

	// If a server is already running, it needs to do nothing.
	if server.canConnect() {
		if err := server.makeSureServerAvailable(); err != nil {
			return err
		}
		server.printlnConsole(fmt.Sprintf("WARN: server is already running on %d port", server.Port))
		return nil
	}

	// If there is authtoken file, it must be old and unnecessary.
	server.deleteAuthtokenIfExists()

	// From here, it has made sure that the port is unused by any other process.

	// Starting up
	if err := server.startInBackground(); err != nil {
		return err
	}
	server.printConsole("Starting server...")

	// Waiting for server up
	for i := 0; ; i++ {
		if i > 100 { // 200 * 100 = 20sec
			server.printlnConsole("")
			return fmt.Errorf("timed out while waiting for server startup")
		}
		server.printConsole(".")
		time.Sleep(200 * time.Millisecond)
		if server.authTokenExists() {
			if alive, _ := server.Alive(); alive {
				break
			}
		}
	}
	server.printlnConsole("") // clear for print
	server.printlnConsole(fmt.Sprintf("Server is successfully started up on %d port", server.Port))
	return nil
}

func (server Server) startInBackground() (err error) {
	log.Println("startInBackground: begin")
	defer func() {
		log.Println("startInBackground: end:", err)
	}()

	// JAVA_HOME (only showing if exists)
	javaHome := cmn.Env("JAVA_HOME", "")
	if len(javaHome) > 0 {
		server.printlnConsole("Java home directory: " + javaHome)
	}

	// GROOVY_HOME
	server.printlnConsole("Groovy home directory: " + GroovyHome)
	groovyCmdPath, err := server.groovyCommand()
	if err != nil {
		return err
	}

	// GroovyServ's home directory and GROOVYSERV_WORK_DIR
	groovyServHome := groovyServHome()
	server.printlnConsole("GroovyServ home directory: " + groovyServHome)
	server.printlnConsole("GroovyServ work directory: " + GroovyServWorkDir)

	// CLASS_PATH
	classpath := filepath.Join(groovyServHome, "lib", "*")
	classpathEnv := os.Getenv("CLASSPATH")
	if len(classpathEnv) > 0 {
		classpath = classpath + cmn.ClasspathDelimiter() + classpathEnv
	}
	server.printlnConsole("Original classpath: " + cmn.Coalesce(classpathEnv, "(none)"))
	server.printlnConsole("GroovyServ default classpath: " + classpath)

	// JAVA_OPTS
	// -server: for performance (experimental)
	// -Djava.awt.headless=true: without this, annoying to switch an active process to it when new process is created as daemon
	javaOpts := "-server -Djava.awt.headless=true " + cmn.Env("JAVA_OPTS", "")

	// Preparing a command
	groovyServOpts := cmn.Env("GROOVYSERV_OPTS", "")
	cmd := exec.Command(groovyCmdPath)
	if len(groovyServOpts) > 0 {
		cmd.Args = append(cmd.Args, groovyServOpts)
	}
	cmd.Args = append(cmd.Args, "-e", "groovyx.groovyserv.GroovyServer.main(args)", "--", fmt.Sprintf("%d", server.Port), server.AuthToken, server.AllowFrom, fmt.Sprintf("%v", server.Verbose))
	for _, arg := range server.Args {
		cmd.Args = append(cmd.Args, arg)
	}
	cmd.Env = []string{
		"CLASSPATH=" + classpath,
		"JAVA_OPTS=" + javaOpts,
	}
	for _, item := range os.Environ() { // must replace an entry not but just append it because it's ignored at specfic platform like windows.
		if strings.HasPrefix(item, "CLASSPATH=") {
			continue // just ignored if exists because it must be replaced surely
		}
		if strings.HasPrefix(item, "JAVA_OPTS=") {
			continue // just ignored if exists because it must be replaced surely
		}
		cmd.Env = append(cmd.Env, item)
	}
	if !server.Quiet {
		cmd.Stdout, cmd.Stderr = os.Stdout, os.Stderr
	}
	log.Printf("startInBackground: command: %#v", cmd)

	// Running the command as another process (doen't block)
	if err := cmd.Start(); err != nil {
		return fmt.Errorf("could not start groovyserver: %s", err.Error())
	}
	return nil
}

func (server Server) connect() (connected *ConnectedServer, err error) {
	log.Println("connect: begin")
	defer func() {
		log.Printf("connect: end: %#v\n, %#v\n", connected, err)
	}()

	conn, err := cmn.Connect(server.Host, server.Port)
	if err != nil {
		return nil, fmt.Errorf("could not connect to server: %s", err.Error())
	}
	return &ConnectedServer{server, conn, false}, nil
}

func (server Server) authToken() (token string, err error) {
	log.Println("authToken: begin")
	defer log.Println("authToken: end")
	defer func() {
		log.Println("authToken: end:", token, err)
	}()

	// User specified token has the highest priority.
	if server.AuthToken != "" {
		return server.AuthToken, nil
	}

	// Reading a stored token from a file.
	body, err := ioutil.ReadFile(server.authTokenFile())
	if err != nil {
		return "", fmt.Errorf("could not read authtoken file: %s", err.Error()) // wrapping message for compatibility with previous version
	}
	authToken := strings.Trim(string(body), "\r\n ")
	if len(body) == 0 {
		return "", fmt.Errorf("empty authtoken")
	}
	log.Printf("authToken: %#v from %s\n", authToken, server.authTokenFile())
	return authToken, nil
}

func (server Server) authTokenFile() string {
	return filepath.Join(GroovyServWorkDir, fmt.Sprintf("authtoken-%d", server.Port))
}

func (server Server) authTokenExists() bool {
	return cmn.FileExists(server.authTokenFile())
}

func (server Server) deleteAuthtokenIfExists() error {
	if server.authTokenExists() {
		authTokenFile := server.authTokenFile()
		server.printlnConsole("WARN: old authtoken file is deleted: " + authTokenFile)
		if err := os.Remove(authTokenFile); err != nil {
			return fmt.Errorf("could not delete old authtoken file %s: %s"+authTokenFile, err)
		}
	}
	return nil
}

func (server Server) groovyCommand() (string, error) {
	groovyCmdName := "groovy"
	if cmn.Windows() {
		groovyCmdName += ".bat"
	}
	if len(GroovyHome) > 0 {
		groovyCmdPath := filepath.Join(GroovyHome, "bin", groovyCmdName)
		if !cmn.FileExists(groovyCmdPath) {
			return "", fmt.Errorf("invalid GROOVY_HOME: %s", GroovyHome)
		}
		server.printlnConsole("Groovy command path: " + groovyCmdPath + " (found at GROOVY_HOME)")
		return groovyCmdPath, nil
	}
	if cmn.CommandExists(groovyCmdName) {
		server.printlnConsole("Groovy command path: " + groovyCmdName + " (found at PATH)")
		return groovyCmdName, nil
	}
	return "", NewHintError("groovy command not found", "Requires either PATH having groovy command or GROOVY_HOME.")
}

func (server Server) printlnConsole(message string) {
	if !server.Quiet {
		fmt.Fprintln(os.Stderr, message)
	}
}

func (server Server) printConsole(message string) {
	if !server.Quiet {
		fmt.Fprint(os.Stderr, message)
	}
}
