@if "%DEBUG%" == "" @echo off

@rem -----------------------------------------------------------------------
@rem Copyright 2009-2013 the original author or authors.
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

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

@rem ----------------------------------------
@rem Parse arguments
@rem ----------------------------------------

:loop_args
    if "%1" == "" (
        goto break_loop_args
    ) else if "%1" == "-v" (
        set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.verbose=true
    ) else if "%1" == "-q" (
        set OPT_QUIET=YES
    ) else if "%1" == "-p" (
        if "%2" == "" (
            echo ERROR: Port number must be specified. >&2
            goto end
        )
        set GROOVYSERVER_PORT=%2
        shift
    ) else if "%1" == "-k" (
        echo ERROR: groovyserver.bat does not support %1. >&2
        goto end
    ) else if "%1" == "-r" (
        echo ERROR: groovyserver.bat does not support %1. >&2
        goto end
    ) else if "%1" == "--allow-from" (
        set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.allowFrom=%2
        shift
    ) else if "%1" == "--authtoken" (
        set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.authtoken=%2
        shift
    ) else (
        echo usage: groovyserver.bat [options]
        echo options:
        echo   -v       verbose output to the log file
        echo   -q       suppress starting messages
        echo  ^(-k       unsupported in groovyserver.bat^)
        echo  ^(-r       unsupported in groovyserver.bat^)
        echo   -p port  specify the port to listen
        goto end
    )
    shift
goto loop_args
:break_loop_args

@rem ----------------------------------------
@rem Support for Cygwin
@rem ----------------------------------------

call :expand_path JAVA_HOME "%JAVA_HOME%"
call :expand_path GROOVY_HOME "%GROOVY_HOME%"
call :expand_path GROOVYSERV_HOME "%GROOVYSERV_HOME%"
call :expand_path CLASSPATH "%CLASSPATH%"

@rem ----------------------------------------
@rem Find groovy command
@rem ----------------------------------------

if defined GROOVY_HOME (
    call :info_log Groovy home directory: "%GROOVY_HOME%"
    call :setup_GROOVY_CMD_from_GROOVY_HOME
    if errorlevel 1 goto end
) else (
    call :info_log Groovy home directory: ^(none^)
    call :setup_GROOVY_CMD_from_PATH
    if errorlevel 1 goto end
)

@rem ----------------------------------------
@rem Resolve GROOVYSERV_HOME
@rem ----------------------------------------

if not defined GROOVYSERV_HOME (
    set GROOVYSERV_HOME=%DIRNAME%..
)
if not exist "%GROOVYSERV_HOME%\lib\groovyserv-*.jar" (
    echo ERROR: Not found a valid GROOVYSERV_HOME directory: "%GROOVYSERV_HOME%" >&2
    goto end
)
call :info_log GroovyServ home directory: "%GROOVYSERV_HOME%"

@rem ----------------------------------------
@rem GroovyServ's work directory
@rem ----------------------------------------

set GROOVYSERV_WORK_DIR=%USERPROFILE%\.groovy\groovyserv
if not exist "%GROOVYSERV_WORK_DIR%" (
    mkdir "%GROOVYSERV_WORK_DIR%"
)
call :info_log GroovyServ work directory: "%GROOVYSERV_WORK_DIR%"

@rem ----------------------------------------
@rem Port and PID and AuthToken
@rem ----------------------------------------

if not defined GROOVYSERVER_PORT (
    set GROOVYSERVER_PORT=1961
)
if defined GROOVYSERV_OPTS (
    set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.port=%GROOVYSERVER_PORT%
) else (
    set GROOVYSERV_OPTS=-Dgroovyserver.port=%GROOVYSERVER_PORT%
)
set GROOVYSERV_AUTHTOKEN_FILE=%GROOVYSERV_WORK_DIR\%authtoken-%GROOVYSERVER_PORT%

@rem ----------------------------------------
@rem Setup classpath
@rem ----------------------------------------

if defined CLASSPATH (
    call :info_log Original classpath: %CLASSPATH%
    set CLASSPATH=%CLASSPATH%;%GROOVYSERV_HOME%\lib\*
) else (
    call :info_log Original classpath: ^(none^)
    set CLASSPATH=%GROOVYSERV_HOME%\lib\*
)
call :info_log GroovyServ default classpath: "%CLASSPATH%"

@rem ----------------------------------------
@rem Setup other variables
@rem ----------------------------------------

@rem -server option for JVM (for performance) (experimental)
if defined JAVA_OPT (
    set JAVA_OPTS=-server %JAVA_OPTS%
) else (
    set JAVA_OPTS=-server
)

@rem -------------------------------------------
@rem Check duplicated invoking
@rem -------------------------------------------

@rem if connecting to server is succeed, return successfully
call :is_server_available
if not errorlevel 1 (
    echo WARN: groovyserver is already running on port %GROOVYSERVER_PORT% >&2
    goto end
)

@rem -------------------------------------------
@rem Invoke server
@rem -------------------------------------------

if exist "%GROOVYSERV_AUTHTOKEN_FILE%" del "%GROOVYSERV_AUTHTOKEN_FILE%"
if defined DEBUG (
    %GROOVY_CMD% %GROOVYSERV_OPTS% -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"
    goto end
) else (
    @rem The start command somehow doesn't update errorleve when it's succeed.
    @rem So before start commaand is invoked, make errorlevel reset explicitly.
    call :reset_errorlevel
    start ^
        "groovyserver[port:%GROOVYSERVER_PORT%]" ^
        /MIN ^
        %GROOVY_CMD% ^
        %GROOVYSERV_OPTS% ^
        -e "println('Groovyserver^(port %GROOVYSERVER_PORT%^) is running');println('Close this window to stop');org.jggug.kobo.groovyserv.GroovyServer.main(args)"
    if errorlevel 1 (
        echo ERROR: Failed to invoke groovyserver >&2
        goto end
    )
)

@rem -------------------------------------------
@rem Wait for available
@rem -------------------------------------------

call :info_log_without_linebreak Starting
:loop_wait_for_available
    call :info_log_without_linebreak .

    @rem if connecting to server is succeed, return successfully
    call :is_server_available
    if not errorlevel 1 (
        goto break_check_invocation
    )
goto loop_wait_for_available
:break_check_invocation
call :info_log_empty

@rem -------------------------------------------
@rem Endpoint
@rem -------------------------------------------

:end
endlocal
exit /B %ERRORLEVEL%

@rem -------------------------------------------
@rem Common function
@rem -------------------------------------------

:info_log
setlocal
    if not "%OPT_QUIET%" == "YES" echo %*
endlocal
exit /B

:info_log_empty
setlocal
    if not "%OPT_QUIET%" == "YES" echo.
endlocal
exit /B

:info_log_without_linebreak
setlocal
    @rem trickey way to echo without newline
    if not "%OPT_QUIET%" == "YES" SET /P X=%*< NUL 1>&2
endlocal
exit /B

@rem ERRORLEVEL will be modified
:is_server_available
    set FIND_CMD=C:\Windows\system32\find
    netstat -an | "%FIND_CMD%" ":%GROOVYSERVER_PORT% " | "%FIND_CMD%" "LISTENING" > NUL 2>&1
exit /B %ERRORLEVEL%

@rem GROOVY_CMD will be modified
:setup_GROOVY_CMD_from_GROOVY_HOME
    set GROOVY_CMD=%GROOVY_HOME%\bin\groovy.bat
    if not exist "%GROOVY_CMD%" (
        echo ERROR: Not found a valid GROOVY_HOME directory: "%GROOVY_HOME%" >&2
        exit /B 1
    )
    call :info_log Groovy command path: "%GROOVY_CMD%" ^(found at GROOVY_HOME^)
exit /B

@rem GROOVY_CMD will be modified
:setup_GROOVY_CMD_from_PATH
    call :find_groovy_from_path_and_setup_GROOVY_CMD groovy.bat
    if not defined GROOVY_CMD (
        echo ERROR: Not found a groovy command. Required either PATH having groovy command or GROOVY_HOME >&2
        exit /B 1
    )
    call :info_log Groovy command path: "%GROOVY_CMD%" ^(found at PATH^)
exit /B

@rem GROOVY_CMD will be modified
:find_groovy_from_path_and_setup_GROOVY_CMD
    @rem Replace long name to short name for start command
    set GROOVY_CMD=%~s$PATH:1
exit /B

@rem ERRORLEVEL will be modified
:reset_errorlevel
exit /B 0

@rem environment variable which name is the first argument will be modified
@rem or when it's a valid windows' long name, it will be converted to the short name.
:expand_path
    set gs_tmp_value=%~s2
    @rem TODO checking a first char need to apply each entry of CLASSPATH
    if "%gs_tmp_value:~0,1%" == "/" (
        for /f "delims=" %%z in ('cygpath.exe --windows --path "%~2"') do (
            set %1=%%z
            call :info_log Expand path:
            call :info_log   -  %1="%gs_tmp_value%"
            call :info_log   +  %1="%%z"
        )
    ) else (
        @rem Replace long name to short name for start command
        set %1=%gs_tmp_value%
    )
exit /B 0

