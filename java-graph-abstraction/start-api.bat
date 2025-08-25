@echo off
chcp 65001 >nul

echo.
echo ╔══════════════════════════════════════╗
echo ║        🌐 Graph Traveler API         ║
echo ║     智能图推理与知识发现系统          ║
echo ╚══════════════════════════════════════╝
echo.

REM 检查 Java
echo 🔍 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到 Java，请先安装 Java 17 或更高版本
    echo 💡 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

REM 检查 Maven
echo 🔍 检查 Maven 环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到 Maven，请先安装 Maven
    echo 💡 下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo ✅ 环境检查通过

REM 设置环境变量
set SPRING_PROFILES_ACTIVE=dev

REM 启动服务
echo.
echo 🚀 正在启动 API 服务...
echo 📍 服务地址: http://localhost:8080
echo 💊 健康检查: http://localhost:8080/api/v1/graph/health
echo 🧪 测试客户端: 打开 test-api.html 文件
echo 📖 完整文档: 查看 API_DOCUMENTATION.md
echo.
echo 🛑 按 Ctrl+C 停止服务
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

mvn spring-boot:run

echo.
echo 👋 服务已停止
pause