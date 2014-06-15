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
	"fmt"
)

const (
	InvalidAuthTokenErrorCode = 201
	ClientNotAllowedErrorCode = 202
)

//----------------------------------------------------------
// HintError

type HintError struct {
	Err  string
	Hint string
}

func NewHintError(err string, hint string) *HintError {
	return &HintError{Err: err, Hint: hint}
}

func (err HintError) Error() string {
	if len(err.Hint) > 0 {
		return err.Err + "\nHint:  " + err.Hint
	}
	return err.Err
}

//----------------------------------------------------------
// InvalidAuthTokenError

type InvalidAuthTokenError struct {
	Err string
}

func NewInvalidAuthTokenError() *InvalidAuthTokenError {
	return &InvalidAuthTokenError{Err: "invalid authtoken"}
}

func (err InvalidAuthTokenError) Error() string {
	return err.Err
}

//----------------------------------------------------------
// ClientNotAllowedError

type ClientNotAllowedError struct {
	Err  string
	Host string
	Port int
}

func NewClientNotAllowedError(host string, port int) *ClientNotAllowedError {
	return &ClientNotAllowedError{
		Err:  fmt.Sprintf("client address not allowed %s:%d", host, port),
		Host: host,
		Port: port,
	}
}

func (err ClientNotAllowedError) Error() string {
	return err.Err
}
