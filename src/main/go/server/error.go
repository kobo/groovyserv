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
	UndefinedErrorCode            = -1
	InvalidAuthTokenErrorCode     = 201
	ClientNotAllowedErrorCode     = 202
	CurrentDirConflictedErrorCode = 203
	IllegalStateErrorCode         = 204
)

type AppError struct {
	Code int
	Err  string
	Hint string
}

func (err AppError) Error() string {
	if len(err.Hint) > 0 {
		return err.Err + "\nHint:  " + err.Hint
	}
	return err.Err
}

func (err AppError) IsInvalidAuthTokenError() bool {
	return err.Code == InvalidAuthTokenErrorCode
}

func (server Server) AppErrorOf(code int) (*AppError, bool) {
	switch code {
	case InvalidAuthTokenErrorCode:
		return &AppError{Code: code, Err: "invalid authtoken", Hint: "Specify a valid authtoken or kill the process somehow."}, true
	case ClientNotAllowedErrorCode:
		return &AppError{Code: code, Err: fmt.Sprintf("client address not allowed %s:%d", server.Host, server.Port)}, true
	case CurrentDirConflictedErrorCode:
		return &AppError{Code: code, Err: "could not change working directory", Hint: "Another thread may be running on a different working directory. Wait a moment."}, true
	case IllegalStateErrorCode:
		return &AppError{Code: code, Err: "illegal state"}, true
	}
	return nil, false
}
