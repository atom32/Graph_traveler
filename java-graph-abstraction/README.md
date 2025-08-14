# Java Graph Database Abstraction Layer

基于ToG-2项目设计的Java图数据库抽象层，支持Neo4j和其他图数据库，具备异步并行推理能力。

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