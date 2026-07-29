@echo off
title PFEP Admin Debug Mode

setlocal enabledelayedexpansion

:: ============ 调试模式密码验证 ============
set "ADMIN_PASSWORD=shjd123456"

echo ================================================
echo   PFEP Coolant Detection System
echo   Admin Debug Mode (start_up_admin.bat)
echo ================================================
echo.
set /p "INPUT_PASS=请输入调试模式密码: "

if not "!INPUT_PASS!"=="%ADMIN_PASSWORD%" (
    echo [FAIL] 密码错误，启动已取消。
    pause
    exit /b 1
)
echo [OK] 密码验证通过，进入调试模式...
echo.
:: =============================================

:: ============ CONFIG ============
:: MySQL 路径（按优先级自动发现，也可手动设置 MYSQL_DIR 环境变量）
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
set APP_MODE=dev
:: ================================

cd /d "%~dp0"

:: --- Step 1: Find Java ---
echo [1] Looking for Java...
set JAVA=
for /f "tokens=*" %%i in ('where java.exe 2^>nul') do if "%JAVA%"=="" set "JAVA=%%i"
if "%JAVA%"=="" if exist "C:\Program Files\Java\jdk-11\bin\java.exe" set "JAVA=C:\Program Files\Java\jdk-11\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA=C:\Program Files\Java\jdk-17\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\Java\jdk-21\bin\java.exe" set "JAVA=C:\Program Files\Java\jdk-21\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\Eclipse Adoptium\jdk-11.0.20.8-hotspot\bin\java.exe" set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-11.0.20.8-hotspot\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin\java.exe" set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin\java.exe"
if "%JAVA%"=="" if exist "C:\Program Files\JetBrains\PyCharm 2024.1.4\jbr\bin\java.exe" set "JAVA=C:\Program Files\JetBrains\PyCharm 2024.1.4\jbr\bin\java.exe"
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
if "%MVN%"=="" if exist "C:\Users\%USERNAME%\AppData\Local\Programs\maven\bin\mvn.cmd" set "MVN=C:\Users\%USERNAME%\AppData\Local\Programs\maven\bin\mvn.cmd"
if "%MVN%"=="" if exist "C:\Program Files\Apache\maven\bin\mvn.cmd" set "MVN=C:\Program Files\Apache\maven\bin\mvn.cmd"
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

:: --- Step 6: Start Backend (debug mode) ---
echo [6] Starting backend (debug mode, port %PORT_BACK%)...
set "JASYPT_ENCRYPTOR_PASSWORD=%JASYPT_KEY%"
if not defined JWT_SECRET set "JWT_SECRET=start-bat-fallback-key-do-not-use-in-production"
if not defined DB_PASSWORD set "DB_PASSWORD=admin123"
set APP_MODE=dev
set SPRING_PROFILES_ACTIVE=dev
start "Backend-%PORT_BACK%" cmd /k "%~dp0backend.bat"
cd /d "%~dp0"

:: --- Step 7: Start Frontend (localhost) ---
echo [7] Starting frontend (localhost, port %PORT_FRONT%)...
cd /d "%~dp0chemical-measurement-frontend"
if not exist "node_modules" (
    echo    First run - installing npm packages...
    call npm install
)
start "Frontend-%PORT_FRONT%" cmd /c "title PFEP Frontend [Admin Debug] && npm run dev"
cd /d "%~dp0"

:: --- Done ---
echo.
echo ================================================
echo   [Admin Debug Mode] Starting...
echo   本机: http://localhost:%PORT_FRONT%
echo   API Docs: http://localhost:%PORT_BACK%/doc.html
echo   所有账号可用（含 dev_admin）
echo ================================================
echo.
echo   Opening browser in 5 seconds...
timeout /t 5 /nobreak >nul
start "" "http://localhost:%PORT_FRONT%" 2>nul
explorer "http://localhost:%PORT_FRONT%" 2>nul
echo.
echo   Close this window to stop all services.
pause
