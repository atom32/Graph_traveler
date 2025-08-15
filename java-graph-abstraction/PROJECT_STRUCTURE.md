# 🌐 Graph Traveler 项目结构

## 📁 目录结构

```
java-graph-abstraction/
├── src/main/java/com/tog/graph/
│   ├── 🏗️ core/                    # 核心抽象层
│   │   ├── Entity.java             # 实体模型
│   │   ├── Relation.java           # 关系模型
│   │   ├── Path.java               # 路径模型
│   │   ├── GraphDatabase.java      # 基础数据库接口
│   │   └── EnhancedGraphDatabase.java # 增强数据库接口
│   │
│   ├── 🔧 service/                 # 业务服务层
│   │   ├── GraphReasoningService.java    # 推理服务
│   │   ├── EntitySearchResult.java       # 搜索结果
│   │   ├── SchemaInfo.java              # Schema信息
│   │   ├── ServiceStatus.java           # 服务状态
│   │   └── ServiceException.java        # 服务异常
│   │
│   ├── 🏭 factory/                 # 工厂层
│   │   ├── GraphServiceFactory.java     # 服务工厂
│   │   └── FactoryException.java        # 工厂异常
│   │
│   ├── 🧠 reasoning/               # 推理逻辑层
│   │   ├── GraphReasoner.java           # 基础推理器
│   │   ├── SchemaAwareGraphReasoner.java # 智能推理器
│   │   ├── MultiHopReasoner.java        # 多跳推理器
│   │   ├── AsyncReasoningExecutor.java  # 异步推理执行器
│   │   ├── ReasoningConfig.java         # 推理配置
│   │   ├── ReasoningResult.java         # 推理结果
│   │   ├── ReasoningPlanner.java        # 推理规划器
│   │   └── parallel/                    # 并行推理组件
│   │
│   ├── 🔍 search/                  # 搜索服务层
│   │   ├── SearchEngine.java            # 搜索引擎接口
│   │   ├── SimpleSearchEngine.java      # 基础搜索引擎
│   │   ├── AdvancedGraphSearchEngine.java # 高级搜索引擎
│   │   ├── ScoredEntity.java            # 评分实体
│   │   └── ScoredRelation.java          # 评分关系
│   │
│   ├── 📊 schema/                  # Schema分析层
│   │   ├── GraphSchemaAnalyzer.java     # Neo4j特定分析器
│   │   ├── DatabaseNeutralSchemaAnalyzer.java # 数据库中立分析器
│   │   ├── GraphSchema.java             # Schema模型
│   │   ├── NodeTypeInfo.java            # 节点类型信息
│   │   ├── RelationshipTypeInfo.java    # 关系类型信息
│   │   ├── PropertyInfo.java            # 属性信息
│   │   └── SearchStrategy.java          # 搜索策略
│   │
│   ├── 🤖 embedding/               # 嵌入服务层
│   │   ├── EmbeddingService.java        # 嵌入服务接口
│   │   └── OpenAIEmbeddingService.java  # OpenAI嵌入实现
│   │
│   ├── 💬 llm/                     # 大语言模型层
│   │   ├── LLMService.java              # LLM服务接口
│   │   └── OpenAIService.java           # OpenAI服务实现
│   │
│   ├── 🗄️ neo4j/                   # Neo4j数据访问层
│   │   └── Neo4jGraphDatabase.java      # Neo4j实现
│   │
│   ├── 🔗 rdf/                     # RDF数据访问层
│   │   └── RdfGraphDatabase.java        # RDF实现
│   │
│   ├── ⚙️ config/                  # 配置层
│   │   └── GraphConfig.java             # 配置管理
│   │
│   ├── 📦 data/                    # 数据加载层
│   │   └── DataLoader.java              # 数据加载器
│   │
│   └── 🎮 demo/                    # 演示层
│       ├── GraphTravelerDemo.java       # 统一演示程序
│       └── DataInitializer.java         # 数据初始化工具
│
├── src/main/resources/
│   └── application-local.properties     # 本地配置文件
│
├── 📜 启动脚本
│   ├── run-graph-traveler.sh           # Linux/Mac启动脚本
│   └── run-graph-traveler.bat          # Windows启动脚本
│
└── 📚 文档
    ├── README.md                        # 项目说明
    ├── USAGE.md                         # 使用指南
    └── PROJECT_STRUCTURE.md            # 项目结构说明
```

## 🏗️ 架构层次

### 8层架构设计：

1. **🎮 表示层 (Presentation Layer)**
   - `demo/` - 用户界面和演示程序

2. **🔧 服务层 (Service Layer)**
   - `service/` - 业务服务封装

3. **🏭 工厂层 (Factory Layer)**
   - `factory/` - 数据库中立的服务创建

4. **🧠 业务逻辑层 (Business Logic Layer)**
   - `reasoning/` - 核心推理逻辑和算法

5. **🔍 搜索服务层 (Search Service Layer)**
   - `search/` - 实体搜索和相似度计算

6. **📊 Schema分析层 (Schema Analysis Layer)**
   - `schema/` - 数据库结构分析和查询策略制定

7. **🤖 外部服务层 (External Service Layer)**
   - `embedding/`, `llm/` - 外部AI服务集成

8. **🗄️ 数据访问层 (Data Access Layer)**
   - `core/`, `neo4j/`, `rdf/` - 数据库抽象和具体实现

9. **⚙️ 基础设施层 (Infrastructure Layer)**
   - `config/`, `data/` - 配置管理和数据加载

## 🎯 设计特点

- ✅ **数据库中立** - 支持Neo4j、RDF等多种数据库
- ✅ **分层架构** - 清晰的职责分离
- ✅ **松耦合设计** - 通过接口实现层间解耦
- ✅ **高度可扩展** - 可以轻松添加新的数据库实现或AI服务
- ✅ **现代化界面** - 统一的演示程序，用户体验友好
- ✅ **完整功能** - 支持实体搜索、推理查询、Schema分析等

## 🚀 快速开始

```bash
# 启动演示程序
./run-graph-traveler.sh

# 或使用Maven
mvn clean compile exec:java
```