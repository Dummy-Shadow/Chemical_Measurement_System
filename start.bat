@echo off
title PFEP Coolant Detection System

:: ============ CONFIG (生产环境请设置环境变量覆盖) ============
::  set DB_PASSWORD=xxx          (覆盖数据库密码)
::  set DB_HOST=192.168.x.x      (MySQL服务器地址，默认localhost)
::  set JWT_SECRET=xxx           (覆盖JWT签名密钥)
::  set JASYPT_ENCRYPTOR_PASSWORD=xxx
::  set CORS_ORIGINS=http://server-ip:3000  (内网部署时设为本机地址)
:: === MySQL 路径（按优先级自动发现，也可手动设置 MYSQL_DIR 环境变量）===
if not defined MYSQL_DIR (
    if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_DIR=C:\Program Files\MySQL\MySQL Server 8.4"
)
if not defined MYSQL_DIR (
    if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_DIR=C:\Program Files\MySQL\MySQL Server 8.0"
)
if not defined MYSQL_DIR (
    if exist "D:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_DIR=D:\Program Files\MySQL\MySQL Server 8.4"
)
if not defined MYSQL_DIR (
    if exist "D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_DIR=D:\Program Files\MySQL\MySQL Server 8.0"
)
if not defined MYSQL_DIR set "MYSQL_DIR=C:\Program Files\MySQL\MySQL Server 8.4"
if not defined MYSQL_DATA (
    if exist "C:\ProgramData\MySQL\MySQL Server 8.4\Data" set "MYSQL_DATA=C:\ProgramData\MySQL\MySQL Server 8.4\Data"
)
if not defined MYSQL_DATA (
    if exist "C:\ProgramData\MySQL\MySQL Server 8.0\Data" set "MYSQL_DATA=C:\ProgramData\MySQL\MySQL Server 8.0\Data"
)
if not defined MYSQL_DATA (
    if exist "D:\ProgramData\MySQL\MySQL Server 8.4\Data" set "MYSQL_DATA=D:\ProgramData\MySQL\MySQL Server 8.4\Data"
)
if not defined MYSQL_DATA (
    if exist "D:\ProgramData\MySQL\MySQL Server 8.0\Data" set "MYSQL_DATA=D:\ProgramData\MySQL\MySQL Server 8.0\Data"
)
if not defined MYSQL_DATA set "MYSQL_DATA=C:\ProgramData\MySQL\MySQL Server 8.4\Data"
set MYSQL_USER=root
set MYSQL_PASS=admin123
set MYSQL_HOST=127.0.0.1
set JASYPT_KEY=pfep-cms-master-key-2026
set PORT_BACK=8090
set PORT_FRONT=3000
:: ================================

cd /d "%~dp0"

echo ================================================
echo   PFEP Coolant Detection System v2.0
echo ================================================
echo.

:: --- Step 1: Find Java ---
echo [1] Looking for Java...
set JAVA=
for /f "tokens=*" %%i in ('where java.exe 2^>nul') do if "%JAVA%"=="" set "JAVA=%%i"
if "%JAVA%"=="" (
    for /d %%d in (
        "C:\Program Files\Java\*"
        "C:\Program Files\Eclipse Adoptium\*"
        "C:\Program Files\JetBrains\*"
    ) do (
        if exist "%%d\bin\java.exe" if "%JAVA%"=="" set "JAVA=%%d\bin\java.exe"
    )
)
if "%JAVA%"=="" (
    echo [FAIL] Java not found. Install JDK 11+
    pause & exit /b 1
)
for %%j in ("%JAVA%") do set "JAVA_HOME=%%~dpj.."
echo    Found: %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: --- Step 2: Find Maven ---
echo [2] Looking for Maven...
set MVN=
for /f "tokens=*" %%i in ('where mvn.cmd 2^>nul') do if "%MVN%"=="" set "MVN=%%i"
if "%MVN%"=="" (
    for /d %%d in (
        "C:\Users\%USERNAME%\AppData\Local\Programs\maven"
        "C:\Program Files\Apache\Maven*"
    ) do (
        if exist "%%d\bin\mvn.cmd" if "%MVN%"=="" set "MVN=%%d\bin\mvn.cmd"
    )
)
if "%MVN%"=="" (
    echo [FAIL] Maven not found. Install Maven 3.9+
    pause & exit /b 1
)
echo    Found: %MVN%

:: --- Step 3: Find Node ---
echo [3] Looking for Node.js...
for /f "tokens=*" %%i in ('where node.exe 2^>nul') do if "%NODE%"=="" set "NODE=%%i"
if "%NODE%"=="" (
    echo [FAIL] Node.js not found. Install Node 16+
    pause & exit /b 1
)
echo    Found: %NODE%

:: --- Step 4: Start MySQL ---
echo [4] Starting MySQL...
tasklist /fi "imagename eq mysqld.exe" 2>nul | find /i "mysqld" >nul
if errorlevel 1 (
    set "MYSQLD=%MYSQL_DIR%\bin\mysqld.exe"
    if not exist "%MYSQLD%" set "MYSQLD=mysqld.exe"
    start /b "" "%MYSQLD%" --standalone --datadir="%MYSQL_DATA%" >nul 2>&1
    timeout /t 4 /nobreak >nul
)
tasklist /fi "imagename eq mysqld.exe" 2>nul | find /i "mysqld" >nul
if errorlevel 1 (
    echo    [WARN] MySQL not detected. Trying service...
    net start MySQL84 2>nul
) else (
    echo    MySQL running
)

:: --- Step 5: Init DB ---
echo [5] Checking database (host: %MYSQL_HOST%)...
if not defined MYSQL_DIR set "MYSQL_DIR=."
"%MYSQL_DIR%\bin\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --host=%MYSQL_HOST% --default-character-set=utf8mb4 -e "SELECT 1 FROM user LIMIT 1" chemical_measurement >nul 2>&1
if errorlevel 1 (
    echo    First run - initializing database...
    "%MYSQL_DIR%\bin\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --host=%MYSQL_HOST% --default-character-set=utf8mb4 < "%~dp0chemical-measurement-backend\sql\init.sql" 2>nul
    echo    Database initialized.
) else (
    echo    Database ready.
)

:: --- Step 6: Start Backend ---
echo [6] Starting backend (port %PORT_BACK%)...
set "JASYPT_ENCRYPTOR_PASSWORD=%JASYPT_KEY%"
if not defined JWT_SECRET set "JWT_SECRET=start-bat-fallback-key-do-not-use-in-production"
if not defined DB_PASSWORD set "DB_PASSWORD=admin123"
set APP_MODE=prod
set SPRING_PROFILES_ACTIVE=prod
if not defined CORS_ORIGINS set "CORS_ORIGINS=http://localhost:3000,http://localhost:3001"
start "Backend-%PORT_BACK%" cmd /k "%~dp0backend.bat"
cd /d "%~dp0"

:: --- Step 7: Start Frontend ---
echo [7] Starting frontend (port %PORT_FRONT%)...
cd /d "%~dp0chemical-measurement-frontend"
if not exist "node_modules" (
    echo    First run - installing npm packages...
    call npm install
)
start "Frontend-%PORT_FRONT%" cmd /c "title PFEP Frontend && npm run dev -- --mode production"
cd /d "%~dp0"

:: --- Done ---
echo.
echo ================================================
echo   PFEP System starting (Production Mode)...
echo.
echo   本机访问:
echo     Frontend:   http://localhost:%PORT_FRONT%
echo     Backend:    http://localhost:%PORT_BACK%
echo     API Docs:   http://localhost:%PORT_BACK%/doc.html
echo.
echo   内网其他电脑访问:
echo     http://<此电脑IP>:%PORT_FRONT%
echo ================================================
echo.
echo   Opening browser in 5 seconds...
timeout /t 5 /nobreak >nul
start "" "http://localhost:%PORT_FRONT%" 2>nul
explorer "http://localhost:%PORT_FRONT%" 2>nul
echo.
echo   Close this window to stop all services.
pause
