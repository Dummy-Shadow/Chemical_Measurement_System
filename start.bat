@echo off & setlocal enabledelayedexpansion
title PFEP车间冷却介质检测数据管理系统
chcp 65001 >nul
color 0A

echo ============================================
echo   PFEP车间冷却介质检测数据管理系统 v2.0
echo   Copyright (c) 2026 郑杭宇
echo ============================================
echo.

:: ========== 配置区（如需修改，改这里） ==========
set MYSQL_DIR=C:\Program Files\MySQL\MySQL Server 8.4
set MYSQL_DATA=C:\ProgramData\MySQL\MySQL Server 8.4\Data
set MYSQL_USER=root
set MYSQL_PASS=admin123
set JASYPT_KEY=pfep-cms-master-key-2026
set SERVER_PORT=8090
set FRONTEND_PORT=3000
:: ================================================

cd /d "%~dp0"

:: 1. 检测 JAVA_HOME
echo [1/5] 检测 Java 环境...
set "JAVA_EXE="
if not "%JAVA_HOME%"=="" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
    for /f "tokens=*" %%i in ('where java.exe 2^>nul') do (
        if not defined JAVA_EXE set "JAVA_EXE=%%i"
    )
)
if not defined JAVA_EXE (
    for /d %%d in (
        "C:\Program Files\Java\jdk*"
        "C:\Program Files\Eclipse Adoptium\jdk*"
        "C:\Program Files\JetBrains\*Charm*\jbr"
    ) do (
        if exist "%%d\bin\java.exe" (
            set "JAVA_EXE=%%d\bin\java.exe"
            set "JAVA_HOME=%%d"
        )
    )
)
if not defined JAVA_EXE (
    echo [错误] 未找到 Java，请安装 JDK 11+ 并设置 JAVA_HOME 环境变量
    pause & exit /b 1
)
echo    Java: !JAVA_EXE!
echo    HOME: !JAVA_HOME!
set "PATH=!JAVA_HOME!\bin;%PATH%"

:: 2. 启动 MySQL
echo [2/5] 检查 MySQL 服务...
tasklist /fi "imagename eq mysqld.exe" 2>nul | findstr mysqld >nul
if errorlevel 1 (
    echo    MySQL 未运行，正在启动...
    if exist "!MYSQL_DIR!\bin\mysqld.exe" (
        start /b "" "!MYSQL_DIR!\bin\mysqld.exe" --standalone --datadir="!MYSQL_DATA!" >nul 2>&1
        timeout /t 4 /nobreak >nul
        tasklist /fi "imagename eq mysqld.exe" 2>nul | findstr mysqld >nul
        if errorlevel 1 (
            :: 尝试作为服务启动
            net start MySQL84 2>nul
        )
    ) else (
        :: 尝试从PATH中找
        for /f "tokens=*" %%i in ('where mysqld.exe 2^>nul') do (
            start /b "" "%%i" --standalone >nul 2>&1
            timeout /t 4 /nobreak >nul
            goto :mysql_ok
        )
    )
)
:mysql_ok
tasklist /fi "imagename eq mysqld.exe" 2>nul | findstr mysqld >nul
if errorlevel 1 (
    echo    [警告] MySQL 启动失败。请手动执行以下命令后重新运行本脚本：
    echo    Start-Process "!MYSQL_DIR!\bin\mysqld.exe" -ArgumentList "--standalone --datadir=!MYSQL_DATA!" -WindowStyle Minimized
    echo    （将提示上传到 GitHub Issues 如无法解决）
) else (
    echo    MySQL 运行中 ✓
)

:: 3. 初始化数据库（首次运行自动建库）
echo [3/5] 检查数据库...
"!MYSQL_DIR!\bin\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --host=127.0.0.1 -e "SELECT 1" chemical_measurement >nul 2>&1
if errorlevel 1 (
    echo    首次运行，正在初始化数据库...
    "!MYSQL_DIR!\bin\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --host=127.0.0.1 --default-character-set=utf8mb4 -e "CREATE DATABASE IF NOT EXISTS chemical_measurement DEFAULT CHARACTER SET utf8mb4;"
    echo    CREATE DATABASE...
    if exist "sql\init.sql" (
        "!MYSQL_DIR!\bin\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --host=127.0.0.1 --default-character-set=utf8mb4 chemical_measurement < "sql\init.sql"
        echo    数据库初始化完成 ✓
    ) else (
        echo    [警告] sql\init.sql 未找到，请手动导入数据库
    )
    echo    测试账号: dev_admin / area_mgr / inspector_a / inspector_b （密码均为 123456）
) else (
    echo    数据库已就绪 ✓
)

:: 4. 启动后端
echo [4/5] 启动后端服务 (端口 %SERVER_PORT%)...
set "JASYPT_ENCRYPTOR_PASSWORD=%JASYPT_KEY%"
cd chemical-measurement-backend
set "MAVEN_HOME="
for /f "tokens=*" %%i in ('where mvn.cmd 2^>nul') do ( if not defined MAVEN_HOME set "MAVEN_HOME=%%i" )
if not defined MAVEN_HOME (
    for /d %%d in ("C:\Users\%USERNAME%\AppData\Local\Programs\maven" "C:\Program Files\Apache\Maven*") do (
        set "MAVEN_HOME=%%d\bin\mvn.cmd"
    )
)
if not defined MAVEN_HOME (
    echo    [错误] 未找到 Maven。请安装 Maven 3.9+
    cd ..
    pause & exit /b 1
)
start "PFEP后端-%SERVER_PORT%" cmd /c "title PFEP后端 & %MAVEN_HOME% spring-boot:run & pause"
cd ..

:: 5. 启动前端
echo [5/5] 启动前端界面 (端口 %FRONTEND_PORT%)...
cd chemical-measurement-frontend
if not exist "node_modules" (
    echo    首次运行，安装前端依赖（约需1-2分钟）...
    call npm install
)
set "NODE_EXE="
for /f "tokens=*" %%i in ('where node.exe 2^>nul') do (
    if not defined NODE_EXE set "NODE_EXE=%%i"
)
if not defined NODE_EXE (
    echo    [错误] 未找到 Node.js。请安装 Node.js 16+
    cd ..
    pause & exit /b 1
)
start "PFEP前端-%FRONTEND_PORT%" cmd /c "title PFEP前端 & npm run dev"
cd ..

:: 6. 打开浏览器
echo.
echo ============================================
echo   启动完成！
echo   前端地址: http://localhost:%FRONTEND_PORT%
echo   后端地址: http://localhost:%SERVER_PORT%
echo   API文档:  http://localhost:%SERVER_PORT%/doc.html
echo ============================================
echo.
echo   测试账号:
echo     开发者   dev_admin / 123456
echo     管理者   area_mgr  / 123456
echo     审核者A  inspector_a / 123456
echo     审核者B  inspector_b / 123456
echo.
echo   按任意键打开浏览器，或关闭本窗口...
pause >nul
start http://localhost:%FRONTEND_PORT%
exit /b 0
