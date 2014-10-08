@if "%DEBUG%" == "" @echo off

@rem -----------------------------------------------------------------------
@rem Copyright 2014 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem     http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem -----------------------------------------------------------------------

setlocal
:begin

@rem ----------------------------------------
@rem Resolving a path
@rem ----------------------------------------

set SCRIPT_PATH=%~f0
for /f %%A in ("%SCRIPT_PATH%") do set PROG_NAME=%%~nxA
if "%PROG_NAME%" == "" set PROG_NAME=setup.bat
set DIR_NAME=%~dp0
if "%DIR_NAME%" == "" set DIR_NAME=.\

@rem ----------------------------------------
@rem Confirming a platform directory existence
@rem ----------------------------------------

set PLATFORM=windows_386
set FROM_DIR="%DIR_NAME%\..\platforms\%PLATFORM%"
if not exist "%FROM_DIR%" (
    echo ERROR: your platform not supported: %PLATFORM% >&2
    echo Sorry, please build by yourself. See http://kobo.github.io/groovyserv/howtobuild.html >&2
    exit /B 1
)

@rem ----------------------------------------
@rem Copying commands
@rem ----------------------------------------

set BIN_DIR="%DIR_NAME%"
copy "%FROM_DIR%\*" "%BIN_DIR%" > NUL

@rem ----------------------------------------
@rem End messages
@rem ----------------------------------------

echo Setup completed successfully for %PLATFORM%
if not "%PROG_NAME%" == "setup.bat" (
    echo It's required only just after installation. Please run the same command once again.
)

@rem ----------------------------------------
@rem Removing dummy scripts
@rem ----------------------------------------

start /B "" cmd /C del /F "%BIN_DIR%\groovyclient" "%BIN_DIR%\groovyclient.bat" "%BIN_DIR%\groovyserver" "%BIN_DIR%\groovyserver.bat" "%BIN_DIR%\setup.sh" "%BIN_DIR%\setup.bat" & exit /B
