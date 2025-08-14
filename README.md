# Graph Traveler

一个基于图数据库的GraphRAG Java实现，提供灵活的图数据库抽象层和智能推理能力。

## 项目概述

Graph Traveler 是一个现代化的GraphRAG解决方案，专为处理复杂的知识图谱查询和推理而设计。项目采用模块化架构，支持多种图数据库后端，并提供异步并行推理能力。

## 核心特性

🚀 **多数据库支持** - 基于抽象层设计，当前支持Neo4j，可轻松扩展到其他图数据库  
⚡ **异步并行推理** - 智能任务分解和并发执行，提升查询性能  
🧠 **智能推理引擎** - 支持多步推理、路径探索和复杂查询规划  
🔌 **LLM集成** - 无缝集成大语言模型，增强语义理解能力  
📊 **性能优化** - 内置缓存、连接池和资源管理机制  
🛠️ **易于扩展** - 清晰的接口设计，便于添加新的数据库支持

## 项目结构

```
Graph_traveler/
├── java-graph-abstraction/     # 核心Java实现
│   ├── src/main/java/com/tog/graph/
│   │   ├── core/              # 抽象接口层
│   │   ├── neo4j/             # Neo4j具体实现
│   │   ├── search/            # 搜索和相似度计算
│   │   ├── reasoning/         # 推理引擎
│   │   ├── llm/              # LLM集成模块
│   │   ├── config/           # 配置管理
│   │   └── demo/             # 示例程序
│   ├── src/test/             # 单元测试
│   └── src/main/resources/   # 配置文件
├── docs/                     # 项目文档
└── examples/                 # 使用示例
```

## 快速开始

### 环境要求

- Java 11+
- Maven 3.6+
- Neo4j 4.0+ (可选，用于Neo4j后端)

### 安装和运行

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd Graph_traveler
   ```

2. **构建项目**
   ```bash
   cd java-graph-abstraction
   mvn clean compile
   ```

3. **配置数据库**
   ```bash
   # 编辑配置文件
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   # 根据你的环境修改数据库连接信息
   ```

4. **运行演示**
   ```bash
   # Linux/Mac
   ./run-demo.sh
   
   # Windows
   run-demo.bat
   
   # 异步演示
   ./run-async-demo.sh
   ```

### 基本使用

```java
// 创建图数据库连接
GraphDatabase database = new Neo4jDatabase(config);

// 初始化推理引擎
ReasoningEngine engine = new AsyncReasoningEngine(database);

// 执行查询
String query = "找到与'人工智能'相关的所有概念及其关系";
ReasoningResult result = engine.reason(query);

// 处理结果
result.getNodes().forEach(node -> {
    System.out.println("节点: " + node.getLabel());
});
```

## 核心模块

### 图数据库抽象层
- `GraphDatabase` - 统一的数据库接口
- `GraphNode` / `GraphRelationship` - 图元素抽象
- 支持事务管理和连接池

### 推理引擎
- `ReasoningEngine` - 推理接口
- `AsyncReasoningEngine` - 异步并行实现
- `ReasoningPlan` - 查询计划生成

### LLM集成
- `LLMService` - 语言模型服务接口
- `OpenAIService` - OpenAI API实现
- 支持嵌入向量和文本生成

## 配置说明

主要配置文件位于 `src/main/resources/application.properties`:

```properties
# Neo4j配置
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=password

# LLM配置
openai.api.key=your-api-key
openai.model=gpt-3.5-turbo

# 推理引擎配置
reasoning.max.parallel.tasks=4
reasoning.timeout.seconds=30
```

## 文档

- [快速开始指南](java-graph-abstraction/QUICK_START_GUIDE.md)
- [配置说明](java-graph-abstraction/CONFIGURATION.md)
- [演示指南](java-graph-abstraction/DEMO_GUIDE.md)
- [技术白皮书](java-graph-abstraction/TECHNICAL_WHITEPAPER.md)
- [部署指南](java-graph-abstraction/SERVICE_DEPLOYMENT_GUIDE.md)

## 开发计划

- [ ] 支持更多图数据库 (ArangoDB, Amazon Neptune)
- [ ] 增强向量搜索能力
- [ ] 添加图可视化组件
- [ ] 提供REST API接口
- [ ] 支持分布式部署

## 贡献

欢迎提交Issue和Pull Request！请确保：

1. 代码符合项目规范
2. 添加适当的测试用例
3. 更新相关文档

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 [GitHub Issue](../../issues)
- 发送邮件至 [email]

---

**Graph Traveler** - 让图数据库查询更智能、更高效
