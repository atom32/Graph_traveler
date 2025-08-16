#!/bin/bash

# Graph Traveler 统一运行脚本
# 适用于 CentOS/Linux 环境

set -e  # 遇到错误立即退出
clear
echo "🌐 Graph Traveler - 智能图推理与知识发现系统"
echo "================================================"

# 检查Java版本
check_java() {
    if ! command -v java &> /dev/null; then
        echo "❌ Java未安装，请先安装Java 17或更高版本"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo "❌ Java版本过低，需要Java 17或更高版本"
        exit 1
    fi
    
    echo "✅ Java版本检查通过"
}

# 检查Maven
check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo "❌ Maven未安装，请先安装Maven"
        exit 1
    fi
    echo "✅ Maven检查通过"
}

# 编译项目
compile_project() {
    echo "🔧 清理并编译项目..."
    mvn clean compile -q
    
    if [ $? -eq 0 ]; then
        echo "✅ 编译成功"
    else
        echo "❌ 编译失败"
        exit 1
    fi
}

# 运行程序
run_program() {
    echo "🚀 启动 Graph Traveler..."
    echo ""
    mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphTravelerDemo" -q
}

# 初始化数据
init_data() {
    echo "📦 初始化测试数据..."
    mvn exec:java -Dexec.mainClass="com.tog.graph.demo.DataInitializer" -q
}

# 显示帮助信息
show_help() {
    echo "用法: ./run.sh [选项]"
    echo ""
    echo "选项:"
    echo "  无参数    - 编译并运行主程序"
    echo "  init      - 初始化测试数据"
    echo "  compile   - 仅编译项目"
    echo "  clean     - 清理编译产物"
    echo "  help      - 显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  ./run.sh          # 运行主程序"
    echo "  ./run.sh init     # 初始化数据"
    echo "  ./run.sh compile  # 仅编译"
}

# 主逻辑
main() {
    case "${1:-}" in
        "help"|"-h"|"--help")
            show_help
            ;;
        "init")
            check_java
            check_maven
            compile_project
            init_data
            ;;
        "compile")
            check_java
            check_maven
            compile_project
            ;;
        "clean")
            echo "🧹 清理编译产物..."
            mvn clean -q
            echo "✅ 清理完成"
            ;;
        "")
            check_java
            check_maven
            compile_project
            run_program
            ;;
        *)
            echo "❌ 未知选项: $1"
            echo "使用 './run.sh help' 查看帮助信息"
            exit 1
            ;;
    esac
}

# 执行主逻辑
main "$@"