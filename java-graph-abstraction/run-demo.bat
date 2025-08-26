@echo off
chcp 65001 >nul

echo.
echo ╔══════════════════════════════════════╗
echo ║     🖥️ Graph Traveler 命令行演示     ║
echo ║     智能图推理与知识发现系统          ║
echo ╚══════════════════════════════════════╝
echo.

REM 检查 Java
echo 🔍 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到 Java，请先安装 Java 17 或更高版本
    pause
    exit /b 1
)

REM 检查 Maven
echo 🔍 检查 Maven 环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到 Maven，请先安装 Maven
    pause
    exit /b 1
)

echo ✅ 环境检查通过

echo.
echo 🚀 启动命令行演示程序...
echo 💡 这是交互式命令行界面
echo 🛑 按 Ctrl+C 或在程序中选择退出
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphTravelerDemo"

echo.
echo 👋 演示程序已退出
pause