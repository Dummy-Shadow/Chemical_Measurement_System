@echo off
title PFEP Backend
echo Starting Spring Boot...
echo JAVA_HOME = %JAVA_HOME%
echo PORT = 8090
cd /d "%~dp0chemical-measurement-backend"
call mvn spring-boot:run
pause
