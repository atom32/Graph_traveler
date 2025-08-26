#!/bin/bash

echo ""
echo "╔══════════════════════════════════════╗"
echo "║     🖥️ Graph Traveler 命令行演示     ║"
echo "║     智能图推理与知识发现系统          ║"
echo "╚══════════════════════════════════════╝"
echo ""

# 检查 Java 版本
echo "🔍 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java，请先安装 Java 17 或更高版本"
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
    exit 1
fi

echo "✅ 环境检查通过"

echo ""
echo "🚀 启动命令行演示程序..."
echo "💡 这是交互式命令行界面"
echo "🛑 按 Ctrl+C 或在程序中选择退出"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphTravelerDemo"

echo ""
echo "👋 演示程序已退出"