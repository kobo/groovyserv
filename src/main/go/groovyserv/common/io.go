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
	"os"
	"path/filepath"
)

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
