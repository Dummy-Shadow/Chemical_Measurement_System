@echo off
title PFEP Deploy - First-Time Setup
setlocal enabledelayedexpansion

echo ================================================
echo   PFEP Coolant Detection System
echo   First-Time Deployment Setup
echo ================================================
echo.

:: ============ Environment Check ============
echo [1/5] Checking Java...
set JAVA=
for /f "tokens=*" %%i in ('where java.exe 2^>nul') do if "%JAVA%"=="" set "JAVA=%%i"
if "%JAVA%"=="" if exist "C:\Program Files\Java\jdk-11\bin\java.exe" set "JAVA=C:\Program Files\Java\jdk-11\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA=C:\Program Files\Java\jdk-17\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\Java\jdk-21\bin\java.exe" set "JAVA=C:\Program Files\Java\jdk-21\bin\java.exe"
if "%JAVA%"=="" if exist "D:\Program Files\Java\jdk-11\bin\java.exe" set "JAVA=D:\Program Files\Java\jdk-11\bin\java.exe"
if "%JAVA%"=="" if exist "D:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA=D:\Program Files\Java\jdk-17\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin\java.exe" set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin\java.exe"
if "%JAVA%"=="" (
    echo [FAIL] JDK 11+ not found. Please install:
    echo   https://adoptium.net/download/
    echo   or run: winget install EclipseAdoptium.Temurin.17.JDK
    pause & exit /b 1
)
for %%j in ("%JAVA%") do set "JAVA_HOME=%%~dpj.."
echo    OK: %JAVA_HOME%

echo [2/5] Checking MySQL...
set MYSQL=
for /f "tokens=*" %%i in ('where mysql.exe 2^>nul') do if "%MYSQL%"=="" set "MYSQL=%%i"
if "%MYSQL%"=="" if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if "%MYSQL%"=="" if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if "%MYSQL%"=="" if exist "D:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL=D:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if "%MYSQL%"=="" if exist "D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL=D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if "%MYSQL%"=="" (
    echo [WARN] MySQL not found in PATH. Checking service...
    sc query MySQL84 >nul 2>&1 && set "MYSQL=mysql.exe" && echo    OK: MySQL84 service found
) else (
    echo    OK: MySQL found
)
if "%MYSQL%"=="" (
    echo [INFO] MySQL not installed on this machine.
    echo    If MySQL is on another server, set env vars:
    echo      set DB_HOST=192.168.x.x
    echo      set DB_PORT=3306
    echo      set DB_USER=root
    echo      set DB_PASSWORD=yourpassword
    echo.
    set /p "HAS_REMOTE_DB=Connect to remote MySQL? (y/n): "
    if /i not "!HAS_REMOTE_DB!"=="y" (
        echo Please install MySQL 8.0+ first:
        echo   https://dev.mysql.com/downloads/installer/
        pause & exit /b 1
    )
    echo    Using remote MySQL - skipping local MySQL startup.
    set "SKIP_MYSQL=1"
) else (
    echo [3/5] Starting MySQL...
    tasklist /fi "imagename eq mysqld.exe" 2>nul | find /i "mysqld" >nul
    if errorlevel 1 (
        net start MySQL84 2>nul
        timeout /t 3 /nobreak >nul
    )
    echo    MySQL ready.
)

:: ============ Configuration ============
echo [4/5] Configuration...
cd /d "%~dp0"

:: Server IP detection
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4.*192\. IPv4.*10\. IPv4.*172\."') do (
    set "SERVER_IP=%%a"
    set "SERVER_IP=!SERVER_IP: =!"
    goto :found_ip
)
:found_ip
if not defined SERVER_IP (
    for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
        set "SERVER_IP=%%a"
        set "SERVER_IP=!SERVER_IP: =!"
        goto :found_ip2
    )
    :found_ip2
)

if not defined SERVER_IP set "SERVER_IP=localhost"

:: Check Maven for packaging
echo    Checking Maven for packaging...
set MVN=
for /f "tokens=*" %%i in ('where mvn.cmd 2^>nul') do if "%MVN%"=="" set "MVN=%%i"
if "%MVN%"=="" if exist "C:\Users\%USERNAME%\AppData\Local\Programs\maven\bin\mvn.cmd" set "MVN=C:\Users\%USERNAME%\AppData\Local\Programs\maven\bin\mvn.cmd"
if "%MVN%"=="" if exist "C:\Program Files\Apache\maven\bin\mvn.cmd" set "MVN=C:\Program Files\Apache\maven\bin\mvn.cmd"

:: Build backend fat JAR if Maven available
if not "%MVN%"=="" (
    echo    Building backend JAR (one-time)...
    cd /d "%~dp0chemical-measurement-backend"
    set "JAVA_HOME=!JAVA_HOME!"
    call "!MVN!" package -DskipTests -q 2>&1
    if exist "target\chemical-measurement-system-1.0.0-SNAPSHOT.jar" (
        set "USE_JAR=1"
        echo    JAR built successfully.
    )
    cd /d "%~dp0"
)

:: Init database
if not defined SKIP_MYSQL (
    echo    Initializing database...
    if not "%MYSQL%"=="" (
        if not defined DB_HOST set "DB_HOST=127.0.0.1"
        if not defined DB_PORT set "DB_PORT=3306"
        if not defined DB_USER set "DB_USER=root"
        if not defined DB_PASSWORD set "DB_PASSWORD=admin123"
        "!MYSQL!" -u%DB_USER% -p%DB_PASSWORD% --host=%DB_HOST% --port=%DB_PORT% --default-character-set=utf8mb4 -e "CREATE DATABASE IF NOT EXISTS chemical_measurement DEFAULT CHARACTER SET utf8mb4" 2>nul
        "!MYSQL!" -u%DB_USER% -p%DB_PASSWORD% --host=%DB_HOST% --port=%DB_PORT% --default-character-set=utf8mb4 -e "SELECT 1 FROM user LIMIT 1" chemical_measurement >nul 2>&1
        if errorlevel 1 (
            "!MYSQL!" -u%DB_USER% -p%DB_PASSWORD% --host=%DB_HOST% --port=%DB_PORT% --default-character-set=utf8mb4 < "%~dp0chemical-measurement-backend\sql\init.sql" 2>nul
            echo    Database created and seeded.
        ) else (
            echo    Database ready.
        )
    )
)

:: ============ Start ============
echo [5/5] Starting system...

:: Set env vars
set "JASYPT_ENCRYPTOR_PASSWORD=pfep-cms-master-key-2026"
if not defined JWT_SECRET set "JWT_SECRET=deploy-secret-change-in-production-32bytes-min"
if not defined CORS_ORIGINS set "CORS_ORIGINS=http://%SERVER_IP%:3000,http://localhost:3000"
set APP_MODE=prod
set SPRING_PROFILES_ACTIVE=prod

:: Start backend
if defined USE_JAR (
    echo    Starting backend (JAR mode)...
    start "Backend-8090" cmd /c "title PFEP Backend && java -jar chemical-measurement-backend\target\chemical-measurement-system-1.0.0-SNAPSHOT.jar"
) else (
    echo    Starting backend (Maven mode)...
    start "Backend-8090" cmd /k "%~dp0backend.bat"
)

:: Start frontend
echo    Starting frontend...
cd /d "%~dp0chemical-measurement-frontend"
if not exist "node_modules" call npm install
start "Frontend-3000" cmd /c "title PFEP Frontend && npm run dev -- --mode production"
cd /d "%~dp0"

:: --- Done ---
echo.
echo ================================================
echo   Deploy Complete!
echo.
echo   Local access:   http://localhost:3000
echo   Intranet access: http://%SERVER_IP%:3000
echo.
echo   Default accounts (password: 123456)
echo     area_mgr      Area Manager
echo     inspector_a   Inspector A
echo     inspector_b   Inspector B
echo.
echo   Note: dev_admin is disabled in production mode
echo================================================
echo.
pause
