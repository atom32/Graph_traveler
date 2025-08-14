#!/bin/bash

echo "=== Testing Embedding Service ==="

# 检查环境变量
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Error: OPENAI_API_KEY environment variable is not set"
    echo "Please set it with: export OPENAI_API_KEY=your_api_key"
    exit 1
fi

echo "✓ OPENAI_API_KEY is set"

# 编译项目
echo "Compiling project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed"
    exit 1
fi

echo "✓ Compilation successful"

# 运行嵌入测试
echo "Running embedding test..."
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.EmbeddingTestDemo" -q

echo "Test completed!"