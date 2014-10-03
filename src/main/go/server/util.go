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
	"os"
	"os/exec"
	"path/filepath"
)

func groovyServWorkDir() string {
	return cmn.ExpandPath(cmn.Env("GROOVYSERV_WORK_DIR", filepath.Join(cmn.HomeDir(), ".groovy", "groovyserv")))
}

func groovyServHome() string {
	// GroovyServ's home directory path should be decided from not an environment variable like GROOVYSERV_HOME but only a path of invoked command.
	// Because to use GROOVYSERV_HOME often causes a confusion that jar files which were installed with a stable version
	// of GroovyServ by GVM are unexpectedly used when you try to use a SNAPSHOT version of GroovyServ in a development.
	cmdPath, err := exec.LookPath(os.Args[0])
	if err != nil {
		panic(fmt.Sprintf("could not resolve a command path: %s", err.Error())) // using panic because here must be unreachable.
	}
	return cmn.ExpandPath(filepath.Join(filepath.Dir(cmdPath), ".."))
}
