@echo off
rem -----------------------------------------------------------------------
rem Copyright 2009-2010 the original author or authors.
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem -----------------------------------------------------------------------

setlocal

rem -----------------------------------
rem Resolve base directory
rem -----------------------------------
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\
set GROOVYSERV_HOME=%DIRNAME%..
rem echo DEBUG: GROOVYSERV_HOME: %GROOVYSERV_HOME%

rem -----------------
rem Parse parameters
rem -----------------
:loop
if "%1" == "" (
    goto break
) else if "%1" == "-v" (
    set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.verbose=true
) else if "%1" == "-p" (
    if "%2" == "" (
        echo ERROR: port number must be specified
        goto end
    )
    set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.port=%2
    shift
) else if "%1" == "-k" (
    echo ERROR: batch file version groovyserver invoker is not support %1.
    goto end
) else if "%1" == "-r" (
    echo ERROR: batch file version groovyserver invoker is not support %1.
    goto end
) else (
    echo Usage: groovyserver [options]
    echo options:
    echo   -v       verbose output. print debugging information etc.
    echo   -q       quiet ^(default^)
    echo   -p port  specify the port for groovyserver
    echo   -k,-r are not supported in batch version of groovyserver invoker.
    echo.
    goto end
)
shift
goto loop
:break
rem echo DEBUG: GROOVYSERV_OPTS: %GROOVYSERV_OPTS%

rem ---------------
rem Set class path
rem ---------------
set CP=%GROOVYSERV_HOME%\lib\jna-3.2.2.jar;
set CP=%GROOVYSERV_HOME%\lib\groovyserv-${project.version}.jar;%CP%
set CLASSPATH=%CP%;%CLASSPATH%
rem echo DEBUG: CLASSPATH: %CLASSPATH%

rem ---------------------------------
rem Compatibility for cygwin (FIXME)
rem ---------------------------------
if "%JAVA_HOME:~0,9%" == "/cygdrive" (
    for /f %%z in ('cygpath.exe -d "%JAVA_HOME%"') do set JAVA_HOME=%%z
)
if "%GROOVY_HOME:~0,9%" == "/cygdrive" (
    for /f %%z in ('cygpath.exe -d "%GROOVY_HOME%"') do set GROOVY_HOME=%%z
)
rem echo DEBUG: JAVA_HOME: %JAVA_HOME%
rem echo DEBUG: GROOVY_HOME: %GROOVY_HOME%

rem -------------
rem Start server
rem -------------

rem --------------------------------------------------------------------
rem Replace long name to short name by using %~s of for cmommand.
rem Because start command can't accept spaces in command line.
rem --------------------------------------------------------------------
for %%A in (%GROOVY_HOME%\bin\groovy) do start "groovyserver" /MIN %%A %GROOVYSERV_OPTS% -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"

rem -------------------------------------------
rem  Wait for available
rem -------------------------------------------

goto check
:loop2

rem trickey way to echo without newline
SET /P X=.< NUL

rem trickey way to wait one second
ping -n 1 127.0.0.1 >NUL

:check
rem if connecting to server is succeed, return successfully
groovyclient --without-invoking-server -e "" > NUL 2>&1
if not errorlevel 1 goto end
goto loop2

:end
