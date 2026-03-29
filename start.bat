@echo off
echo ===================================
echo   矿物识别系统后端 - 启动脚本
echo ===================================
echo.

REM 检查 Java 环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Java 环境，请先安装 JDK 17+
    pause
    exit /b 1
)

echo [信息] 正在启动服务...
echo.

REM 启动服务
mvn spring-boot:run

pause
