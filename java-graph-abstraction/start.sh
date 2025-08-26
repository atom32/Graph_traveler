#!/bin/bash

echo ""
echo "╔══════════════════════════════════════╗"
echo "║        🌐 Graph Traveler             ║"
echo "║     智能图推理与知识发现系统          ║"
echo "╚══════════════════════════════════════╝"
echo ""
echo "请选择运行方式:"
echo ""
echo "1. 🌐 REST API 服务 (推荐用于集成开发)"
echo "   - 提供 HTTP API 接口"
echo "   - 支持多客户端并发访问"
echo "   - 可被其他应用程序调用"
echo ""
echo "2. 🖥️ 命令行演示程序 (推荐用于学习测试)"
echo "   - 交互式命令行界面"
echo "   - 直观的功能演示"
echo "   - 适合学习和调试"
echo ""
echo "3. 📊 初始化测试数据"
echo ""
echo "0. 退出"
echo ""
read -p "请输入选择 (0-3): " choice

case $choice in
    1)
        echo ""
        echo "🚀 启动 REST API 服务..."
        chmod +x start-api.sh
        ./start-api.sh
        ;;
    2)
        echo ""
        echo "🚀 启动命令行演示程序..."
        chmod +x run-demo.sh
        ./run-demo.sh
        ;;
    3)
        echo ""
        echo "📊 初始化测试数据..."
        mvn exec:java -Dexec.mainClass="com.tog.graph.demo.DataInitializer"
        ;;
    0)
        echo "👋 再见!"
        exit 0
        ;;
    *)
        echo "❌ 无效选择，请重新运行"
        exit 1
        ;;
esac