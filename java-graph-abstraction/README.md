# 🌐 Graph Traveler

智能图推理与知识发现系统 - 基于ToG-2项目设计的现代化图数据库抽象层，支持多种图数据库和智能推理能力。

## 🚀 快速启动

> 📖 **新用户请先查看：[快速启动指南](QUICK_START.md)**

### 方式一：REST API 服务 (推荐)

```bash
# Windows 用户
start-api.bat

# Linux/Mac 用户
chmod +x start-api.sh
./start-api.sh

# 或使用 Maven
mvn spring-boot:run
```

**API 服务地址：** http://localhost:8080

- 🔍 健康检查: `GET /api/v1/graph/health`
- 📖 完整 API 文档: 查看 `API_DOCUMENTATION.md`
- 🧪 测试客户端: 打开 `test-api.html` 文件

### 方式二：命令行演示程序

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
# 运行 API 服务
mvn spring-boot:run

# 运行命令行演示
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphTravelerDemo"

# 初始化数据
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.DataInitializer"
```

## 📋 功能特性

### 🌐 REST API 服务
- 🔗 **RESTful API** - 基于 Spring Boot 的现代化 API 服务
- 🔍 **实体搜索 API** - `/api/v1/graph/search/entities`
- 🧠 **智能推理 API** - `/api/v1/graph/reasoning/schema-aware`
- 🤖 **多智能体协作 API** - `/api/v1/graph/agents/collaborative-query`
- 📊 **Schema 信息 API** - `/api/v1/graph/schema`
- 💊 **健康检查 API** - `/api/v1/graph/health`

### 🧠 核心推理能力
- 🔍 **智能实体搜索** - 基于语义向量的实体发现
- 🤔 **标准推理查询** - 基于图结构的逻辑推理
- 🧠 **智能推理查询** - 基于Schema的自动查询规划
- 🤖 **多智能体协作** - 专业化智能体分工合作
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

## 🔧 API 使用示例

### cURL 示例

```bash
# 健康检查
curl http://localhost:8080/api/v1/graph/health

# 实体搜索
curl -X POST http://localhost:8080/api/v1/graph/search/entities \
  -H "Content-Type: application/json" \
  -d '{"query": "张仲景", "limit": 5}'

# 智能推理查询
curl -X POST http://localhost:8080/api/v1/graph/reasoning/schema-aware \
  -H "Content-Type: application/json" \
  -d '{"question": "张仲景与伤寒论的关系？"}'

# 多智能体协作查询
curl -X POST http://localhost:8080/api/v1/graph/agents/collaborative-query \
  -H "Content-Type: application/json" \
  -d '{"query": "分析张机与王叔和的关系"}'
```

### JavaScript 示例

```javascript
// 实体搜索
const searchResult = await fetch('http://localhost:8080/api/v1/graph/search/entities', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ query: '张仲景', limit: 10 })
});

// 智能推理
const reasoningResult = await fetch('http://localhost:8080/api/v1/graph/reasoning/schema-aware', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ question: '张仲景与伤寒论的关系？' })
});
```

## 核心特性

- **异步并行推理** - 类似MCP的步骤分解和并行执行
- **图数据库抽象层** - 支持多种图数据库
- **智能推理规划** - 根据问题类型生成最优执行计划
- **多步推理和路径探索** - 深度图遍历
- **LLM集成** - OpenAI API支持
- **性能优化** - 并发执行和资源管理
- **RESTful API** - 现代化的 Web API 接口