REM Copyright 2009-2010 the original author or authors.
REM
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM     http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM

setlocal

@rem Determine what directory it is in.
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

set GROOVYSERV_HOME=%DIRNAME%..

set CP=%GROOVYSERV_HOME%\lib\jna-3.2.2.jar;%CP%
set CP=%GROOVYSERV_HOME%\lib\groovyserv-${project.version}.jar;%CP%

"%GROOVY_HOME%"\bin\groovy -cp "%CP%" %GROOVYSERV_OPTS% -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"
