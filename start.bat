@echo off
setlocal enabledelayedexpansion
title PFEP Coolant Detection System

:: ============ CONFIG ============
set MYSQL_DIR=C:\Program Files\MySQL\MySQL Server 8.4
set MYSQL_DATA=C:\ProgramData\MySQL\MySQL Server 8.4\Data
set MYSQL_USER=root
set MYSQL_PASS=admin123
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
for /f "tokens=*" %%i in ('where java.exe 2^>nul') do if "!JAVA!"=="" set "JAVA=%%i"
if "%JAVA%"=="" (
    for /d %%d in (
        "C:\Program Files\Java\*"
        "C:\Program Files\Eclipse Adoptium\*"
        "C:\Program Files\JetBrains\*"
    ) do (
        if exist "%%d\bin\java.exe" if "!JAVA!"=="" set "JAVA=%%d\bin\java.exe"
    )
)
if "%JAVA%"=="" (
    echo [FAIL] Java not found. Install JDK 11+
    pause & exit /b 1
)
for %%j in ("%JAVA%") do set "JAVA_HOME=%%~dpj.."
echo    Found: %JAVA%
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: --- Step 2: Find Maven ---
echo [2] Looking for Maven...
set MVN=
for /f "tokens=*" %%i in ('where mvn.cmd 2^>nul') do if "!MVN!"=="" set "MVN=%%i"
if "%MVN%"=="" (
    for /d %%d in (
        "C:\Users\%USERNAME%\AppData\Local\Programs\maven"
        "C:\Program Files\Apache\Maven*"
    ) do (
        if exist "%%d\bin\mvn.cmd" if "!MVN!"=="" set "MVN=%%d\bin\mvn.cmd"
    )
)
if "%MVN%"=="" (
    echo [FAIL] Maven not found. Install Maven 3.9+
    pause & exit /b 1
)
echo    Found: %MVN%

:: --- Step 3: Find Node ---
echo [3] Looking for Node.js...
set NODE=
for /f "tokens=*" %%i in ('where node.exe 2^>nul') do if "!NODE!"=="" set "NODE=%%i"
if "%NODE%"=="" (
    echo [FAIL] Node.js not found. Install Node 16+
    pause & exit /b 1
)
for %%n in ("%NODE%") do set "NPM=%%~dpnnpm.cmd"
echo    Found: %NODE%

:: --- Step 4: Start MySQL ---
echo [4] Starting MySQL...
tasklist /fi "imagename eq mysqld.exe" 2>nul | find /i "mysqld" >nul
if errorlevel 1 (
    set "MYSQLD=%MYSQL_DIR%\bin\mysqld.exe"
    if not exist "!MYSQLD!" set "MYSQLD=mysqld.exe"
    start /b "" "!MYSQLD!" --standalone --datadir="%MYSQL_DATA%" >nul 2>&1
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
echo [5] Checking database...
"%MYSQL_DIR%\bin\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --host=127.0.0.1 --default-character-set=utf8mb4 -e "SELECT 1 FROM user LIMIT 1" chemical_measurement >nul 2>&1
if errorlevel 1 (
    echo    First run - initializing database...
    "%MYSQL_DIR%\bin\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --host=127.0.0.1 --default-character-set=utf8mb4 < "%~dp0chemical-measurement-backend\sql\init.sql" 2>nul
    echo    Database initialized.
) else (
    echo    Database ready.
)

:: --- Step 6: Start Backend ---
echo [6] Starting backend (port %PORT_BACK%)...
set JASYPT_ENCRYPTOR_PASSWORD=%JASYPT_KEY%
set "BACKEND_DIR=%~dp0chemical-measurement-backend"
start "Backend-%PORT_BACK%" cmd /k "%~dp0backend.bat" "%JAVA_HOME%" "%JASYPT_KEY%" "%BACKEND_DIR%"
cd /d "%~dp0"

:: --- Step 7: Start Frontend ---
echo [7] Starting frontend (port %PORT_FRONT%)...
cd /d "%~dp0chemical-measurement-frontend"
if not exist "node_modules" (
    echo    First run - installing npm packages...
    call npm install
)
start "Frontend-%PORT_FRONT%" cmd /c "title PFEP Frontend && npm run dev"
cd /d "%~dp0"

:: --- Done ---
echo.
echo ================================================
echo   System starting...
echo   Backend:    http://localhost:%PORT_BACK%
echo   Frontend:   http://localhost:%PORT_FRONT%
echo   API Docs:   http://localhost:%PORT_BACK%/doc.html
echo.
echo   Accounts (password: 123456)
echo     dev_admin / area_mgr / inspector_a / inspector_b
echo ================================================
echo.
echo   Opening browser in 5 seconds...
timeout /t 5 /nobreak >nul
start "" "http://localhost:%PORT_FRONT%" 2>nul
explorer "http://localhost:%PORT_FRONT%" 2>nul
echo.
echo   Close this window to stop all services.
pause
