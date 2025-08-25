#!/bin/bash

echo ""
echo "╔══════════════════════════════════════╗"
echo "║        🌐 Graph Traveler API         ║"
echo "║     智能图推理与知识发现系统          ║"
echo "╚══════════════════════════════════════╝"
echo ""

# 检查 Java 版本
echo "🔍 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java，请先安装 Java 17 或更高版本"
    echo "💡 下载地址: https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ 错误: 需要 Java 17 或更高版本，当前版本: $JAVA_VERSION"
    exit 1
fi

# 检查 Maven
echo "🔍 检查 Maven 环境..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未找到 Maven，请先安装 Maven"
    echo "💡 下载地址: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "✅ 环境检查通过"

# 设置环境变量
export SPRING_PROFILES_ACTIVE=dev

# 启动服务
echo ""
echo "🚀 正在启动 API 服务..."
echo "📍 服务地址: http://localhost:8080"
echo "💊 健康检查: http://localhost:8080/api/v1/graph/health"
echo "🧪 测试客户端: 打开 test-api.html 文件"
echo "📖 完整文档: 查看 API_DOCUMENTATION.md"
echo ""
echo "🛑 按 Ctrl+C 停止服务"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

mvn spring-boot:run

echo ""
echo "👋 服务已停止"