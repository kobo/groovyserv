@echo off

set CLASSPATH=src\main\groovy
set OPT="-Dgroovyserver.verbose=true"
groovy.bat %OPT% src/main/groovy/org/jggug/kobo/groovyserv/GroovyServer.groovy

