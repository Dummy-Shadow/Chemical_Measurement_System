@echo off
title PFEP Backend
set "JAVA_HOME=%~1"
set "PATH=%~1\bin;%PATH%"
set "JASYPT_ENCRYPTOR_PASSWORD=%~2"
cd /d "%~3"
mvn spring-boot:run
pause
