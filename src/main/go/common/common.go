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
package common

import (
	"bufio"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
)

//--------------------
// strings

func ArrayAny(slice []string, expected ...string) bool {
	for _, v := range slice {
		for _, e := range expected {
			if v == e {
				return true
			}
		}
	}
	return false
}

func ArrayFuncAny(slice []string, condition func(string) bool) bool {
	for _, v := range slice {
		if condition(v) {
			return true
		}
	}
	return false
}

func Coalesce(slice ...string) string {
	for _, v := range slice {
		if len(v) > 0 {
			return v
		}
	}
	return ""
}

func Termary(condition bool, trueValue string, falseValue string) string {
	if condition {
		return trueValue
	} else {
		return falseValue
	}
}

//--------------------
// io

func ReadLine(rd *bufio.Reader) (string, error) {
	lineBytes, isPrefix, err := rd.ReadLine()
	line := string(lineBytes)
	if len(line) == 0 {
		return line, nil
	}
	if err != nil {
		return line, err
	}
	log.Printf("Read: %s (prefix:%v)\n", line, isPrefix)
	return line, nil
}

func WriteLine(writer io.Writer, text string) {
	Write(writer, text+"\n")
}

func Write(writer io.Writer, text string) {
	_, err := writer.Write([]byte(text))
	if err != nil {
		panic(fmt.Sprintf("could not write: %s", err.Error())) // TODO It seems a bad way to use a panic in utility code
	}
	log.Println("Wrote: ", text)
}

func FileExists(name string) bool {
	_, err := os.Stat(name)
	return !os.IsNotExist(err)
}

func ExpandPath(path string) string {
	if filepath.IsAbs(path) {
		return path
	}
	wd, _ := os.Getwd()
	return filepath.Join(wd, path)
}

//--------------------
// net

func Connect(host string, port int) (net.Conn, error) {
	servAddr := fmt.Sprintf("%s:%d", host, port)
	tcpAddr, err := net.ResolveTCPAddr("tcp", servAddr)
	if err != nil {
		return nil, err
	}
	conn, err := net.DialTCP("tcp", nil, tcpAddr)
	if err != nil {
		return nil, err
	}
	return conn, nil
}

//--------------------
// script/platform

type Args []string

func (args Args) Param(argIndex int, option string) (paramIndex int, param string) {
	paramIndex = argIndex+1
	if len(args) <= paramIndex {
		panic(fmt.Sprintf("option %s requires a parameter", option)) // TODO It seems a bad way to use a panic in utility code
	}
	param = args[paramIndex]
	return
}

func HomeDir() string {
	if Windows() {
		return os.Getenv("USERPROFILE")
	}
	return os.Getenv("HOME")
}

func ClasspathDelimiter() string {
	return Termary(Windows(), ";", ":")
}

func Windows() bool {
	return runtime.GOOS == "windows"
}

func Env(name string, defaultValue string) string {
	value := os.Getenv(name)
	if len(value) == 0 {
		return defaultValue
	}
	return value
}

func EnvInt(name string, defaultValue int) int {
	rawValue := os.Getenv(name)
	if len(rawValue) == 0 {
		return defaultValue
	}
	value, err := strconv.Atoi(rawValue)
	if err != nil {
		fmt.Printf("WARN: environment variable %s requires a number: %s\n", name, rawValue)
		return defaultValue
	}
	return value
}

func CommandExists(name string) bool {
	// If there is the command in PATH, Command.Path() returns its full path.
	return exec.Command(name).Path != name
}

func Dump(obj interface{}) {
	fmt.Printf("%#v\n", obj)
}

func ExitIfPanic() {
	if r := recover(); r != nil {
		fmt.Fprintf(os.Stderr, "ERROR: %s\n", r)
		os.Exit(1)
	}
}

//--------------------
// groovyserv

func GroovyServWorkDir() string {
	return ExpandPath(Env("GROOVYSERV_WORK_DIR", filepath.Join(HomeDir(), ".groovy", "groovyserv")))
}

func GroovyServHome(commandPath string) string {
	return ExpandPath(Env("GROOVYSERV_HOME", filepath.Join(filepath.Dir(commandPath), "..")))
}
