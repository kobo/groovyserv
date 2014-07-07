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
@rem Save script path
@rem ----------------------------------------

set SCRIPT_PATH=%~f0
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

@rem ----------------------------------------
@rem Confirming a platform directory existence
@rem ----------------------------------------

set PLATFORM=windows_386
set FROM_DIR="%DIRNAME%\..\platforms\%PLATFORM%"
if not exist "%FROM_DIR%" (
    echo ERROR: your platform not supported: %PLATFORM% >&2
    echo Sorry, please build by yourself. See http://kobo.github.io/groovyserv/howtobuild.html >&2
    exit /B 1
)

@rem ----------------------------------------
@rem Copying commands
@rem ----------------------------------------

set BIN_DIR="%DIRNAME%"
copy "%FROM_DIR%\*" "%BIN_DIR%"

echo Setup completed successfully for %PLATFORM%

