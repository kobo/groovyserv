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

func ReadLine(reader *bufio.Reader) (line string, err error) {
	var lineBytes, buf []byte
	var isPrefix = true
	for isPrefix && err == nil {
		buf, isPrefix, err = reader.ReadLine()
		lineBytes = append(lineBytes, buf...)
		log.Printf("ReadLine: %#v (prefix:%v)\n", string(lineBytes), isPrefix)
	}
	line = string(lineBytes)
	return
}

func WriteLine(writer io.Writer, text string) error {
	return Write(writer, text+"\n")
}

func Write(writer io.Writer, text string) error {
	_, err := writer.Write([]byte(text))
	if err != nil {
		return fmt.Errorf("could not write: %s", err.Error())
	}
	log.Println("Wrote: ", text)
	return nil
}

func FileExists(name string) bool {
	_, err := os.Stat(name)
	return !os.IsNotExist(err)
}

func ExpandPath(path string) string {
	symlinkSrcPath, _ := filepath.EvalSymlinks(path)
	if symlinkSrcPath != path {
		return symlinkSrcPath
	}
	if filepath.IsAbs(path) {
		return path
	}
	wd, _ := os.Getwd()
	return filepath.Join(wd, path)
}
