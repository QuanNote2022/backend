@echo off
echo ===================================
echo   矿物识别系统后端 - 打包脚本
echo ===================================
echo.

REM 检查 Maven 环境
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Maven 环境，请先安装 Maven
    pause
    exit /b 1
)

echo [信息] 正在清理并编译项目...
call mvn clean

echo [信息] 正在打包项目...
call mvn package -DskipTests

echo.
echo [完成] 打包完成！
echo [位置] target/mineralDO-system-1.0.0.jar
echo.

pause
