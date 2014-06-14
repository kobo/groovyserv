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
	"fmt"
	"os"
	"os/exec"
	"strconv"
)

type Args []string

func (args Args) Param(argIndex int, option string) (paramIndex int, param string) {
	paramIndex = argIndex+1
	if len(args) <= paramIndex {
		panic(fmt.Sprintf("option %s requires a parameter", option)) // TODO It seems a bad way to use a panic in utility code
	}
	param = args[paramIndex]
	return
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
