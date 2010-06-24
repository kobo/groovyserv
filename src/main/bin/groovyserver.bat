@echo off
@rem Copyright 2009-2010 the original author or authors.
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
@rem

setlocal

@rem Determine what directory it is in.
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

set GROOVYSERV_HOME=%DIRNAME%..

:loop
if "%1"=="" goto break
if "%1"=="-q" set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.verbose=false
if "%1"=="-v" set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.verbose=true
if NOT "%1"=="-p" goto no_p
  shift
  set GROOVYSERV_OPTS=%GROOVYSERV_OPTS% -Dgroovyserver.port=%1
:no_p
shift
goto loop
:break

set CP=%GROOVYSERV_HOME%\lib\jna-3.2.2.jar;%CP%
set CP=%GROOVYSERV_HOME%\lib\groovyserv-${project.version}.jar;%CP%

echo "%GROOVY_HOME%"\bin\groovy -cp "%CP%" %GROOVYSERV_OPTS% -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"

"%GROOVY_HOME%"\bin\groovy -cp "%CP%" %GROOVYSERV_OPTS% -e "org.jggug.kobo.groovyserv.GroovyServer.main(args)"


