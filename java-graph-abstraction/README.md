# 🌐 Graph Traveler

智能图推理与知识发现系统 - 基于ToG-2项目设计的现代化图数据库抽象层，支持多种图数据库和智能推理能力。

## 🚀 快速启动

### 运行统一演示程序

```bash
# 赋予执行权限
chmod +x run.sh

# 运行主程序
./run.sh

# 初始化测试数据
./run.sh init

# 仅编译项目
./run.sh compile

# 查看帮助
./run.sh help
```

### 或者使用Maven直接运行

```bash
# 编译并运行
mvn clean compile exec:java

# 初始化数据
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.DataInitializer"
```

## 📋 功能特性

- 🔍 **智能实体搜索** - 基于语义向量的实体发现
- 🤔 **标准推理查询** - 基于图结构的逻辑推理
- 🧠 **智能推理查询** - 基于Schema的自动查询规划
- 📊 **Schema分析** - 自动数据库结构分析和优化建议
- 🔧 **数据库中立** - 支持Neo4j、RDF等多种图数据库
- ⚡ **高性能** - 支持异步并行推理

## 项目结构

```
src/
├── main/java/com/tog/graph/
│   ├── core/           # 核心抽象接口
│   ├── neo4j/          # Neo4j实现
│   ├── search/         # 检索和相似度计算
│   ├── reasoning/      # 推理引擎（支持异步并行）
│   ├── llm/           # LLM集成
│   ├── config/        # 配置管理
│   └── demo/          # 演示程序
├── test/              # 测试用例
└── resources/         # 配置文件
```

## 核心特性

- **异步并行推理** - 类似MCP的步骤分解和并行执行
- **图数据库抽象层** - 支持多种图数据库
- **智能推理规划** - 根据问题类型生成最优执行计划
- **多步推理和路径探索** - 深度图遍历
- **LLM集成** - OpenAI API支持
- **性能优化** - 并发执行和资源管理