# Graph Traveler

一个基于图数据库的GraphRAG Java实现，提供灵活的图数据库抽象层和智能推理能力。

## 项目概述

Graph Traveler 是一个现代化的GraphRAG解决方案，专为处理复杂的知识图谱查询和推理而设计。项目采用模块化架构，支持多种图数据库后端，并提供异步并行推理能力。

## 核心特性

🚀 **多数据库支持** - 基于抽象层设计，当前支持Neo4j，可轻松扩展到其他图数据库  
⚡ **异步并行推理** - 智能任务分解和并发执行，提升查询性能  
🧠 **Schema感知智能推理** - 基于数据库结构的智能实体抽取和查询规划  
🤖 **多智能体协作系统** - 专业化智能体分工协作，透明化推理过程  
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

#### Schema感知智能推理
```java
// 创建推理服务
GraphReasoningService reasoningService = GraphServiceFactory.createGraphReasoningService(config);

// 执行Schema感知推理
String question = "张仲景与伤寒论的关系？";
ReasoningResult result = reasoningService.performSchemaAwareReasoning(question);

// 查看推理结果
System.out.println("答案: " + result.getAnswer());
result.getEvidences().forEach(evidence -> {
    System.out.println("证据: " + evidence);
});
```

#### 多智能体协作查询
```java
// 初始化多智能体系统
MultiAgentCoordinator coordinator = new MultiAgentCoordinator();
coordinator.registerAgent(new EntitySearchAgent(searchEngine));
coordinator.registerAgent(new RelationshipAnalysisAgent(database));

// 执行协作查询
AgentResult searchResult = coordinator.executeTask("entity_search", "张仲景", 
    Map.of("limit", 10));

AgentResult analysisResult = coordinator.executeTask("relationship_analysis", "分析关系",
    Map.of("entity_id", entityId));

// 查看智能体工作结果
System.out.println("搜索结果: " + searchResult.getResult());
System.out.println("关系分析: " + analysisResult.getResult());
```

#### 统一演示程序
```java
// 运行完整演示
GraphTravelerDemo demo = new GraphTravelerDemo();
demo.run();

// 支持的查询类型：
// 1. 🧠 智能推理查询 - "张机与吴普的关系？"
// 2. 🤖 多智能体协作 - 专业化分工处理
// 3. 🔍 实体搜索 - "张仲景"
// 4. 📊 Schema分析 - 数据库结构分析
```

## 核心模块

### 图数据库抽象层
- `GraphDatabase` - 统一的数据库接口
- `GraphNode` / `GraphRelationship` - 图元素抽象
- 支持事务管理和连接池

### 智能推理引擎
- `SchemaAwareGraphReasoner` - Schema感知的智能推理器
- `GraphReasoner` - 标准推理引擎
- `ReasoningPlan` - 查询计划生成
- `MultiHopReasoner` - 多跳推理实现

### 多智能体协作系统
- `MultiAgentCoordinator` - 智能体协调器
- `EntitySearchAgent` - 实体搜索专家
- `RelationshipAnalysisAgent` - 关系分析专家
- `ReasoningCoordinatorAgent` - 推理协调器

### LLM集成
- `LLMService` - 语言模型服务接口
- `OpenAIService` - OpenAI API实现
- 支持嵌入向量和文本生成

## 🧠 智能推理查询技术详解

### Schema感知推理架构

Graph Traveler 的智能推理查询基于 `SchemaAwareGraphReasoner`，实现了业界领先的Schema感知推理技术：

#### 1. **智能实体抽取**
```java
// 多策略融合的实体抽取
1. LLM增强抽取 - 结合大语言模型和Schema上下文
2. Schema模式匹配 - 基于数据库结构的实体识别
3. 语义相似度计算 - 向量嵌入的实体匹配
4. 中文优化处理 - 针对中文文本的特殊优化
```

**核心特性**：
- **动态类型推断**：基于Schema自动推断实体类型
- **置信度评分**：多维度计算实体抽取的可信度
- **搜索属性优化**：根据Schema选择最优搜索字段

#### 2. **智能查询规划**
```java
// QueryPlan 设计模式
- EntitySearch步骤：针对每个抽取实体的搜索策略
- RelationshipTraversal步骤：关系遍历和路径探索
- 置信度传播：步骤间的置信度计算和传播
- 查询意图识别：自动识别用户查询意图
```

#### 3. **多跳推理执行**
```java
// 深度优先 + 广度优先混合策略
- 可配置推理深度（默认3跳）
- 关系相关性评分和过滤
- 路径权重计算和排序
- 间接连接发现算法
```

**推理优化机制**：
- **深度惩罚**：越深的关系分数越低，避免噪声
- **类型权重**：基于Schema动态计算关系重要性
- **宽度控制**：限制每层探索实体数，平衡效果和性能

#### 4. **可解释推理过程**
```java
// 完整的推理链路追踪
- ReasoningStep：记录每步推理操作
- Evidence收集：收集支撑结论的证据链
- 置信度传播：跟踪推理过程中的置信度变化
- 路径可视化：生成推理路径的可视化表示
```

## 🤖 多智能体协作查询技术详解

### 专业化智能体架构

多智能体系统基于 `MultiAgentCoordinator` 实现专业化分工和协作：

#### 1. **EntitySearchAgent - 实体搜索专家**
```java
// 多策略搜索引擎
支持任务类型：
- entity_search：标准实体搜索
- entity_identification：文本实体识别  
- semantic_search：语义相似度搜索

搜索策略：
1. 精确匹配搜索
2. 部分匹配搜索（降权处理）
3. 单字符搜索（中文优化）
4. 语义向量搜索
```

**核心算法**：
- **去重合并**：基于实体ID的智能去重
- **分数融合**：多策略搜索结果的分数融合
- **阈值过滤**：动态调整相似度阈值

#### 2. **RelationshipAnalysisAgent - 关系分析专家**
```java
// 图结构分析引擎
支持任务类型：
- relationship_analysis：实体关系统计分析
- path_finding：实体间路径发现
- connection_discovery：连接模式发现
- relation_summary：关系类型统计

分析算法：
1. BFS路径搜索算法
2. 关系类型聚类分析
3. 连接强度计算
4. 图结构模式识别
```

**高级特性**：
- **多跳连接**：发现间接关系连接
- **路径排序**：基于路径长度和权重排序
- **模式识别**：识别常见的连接模式

#### 3. **智能协调机制**
```java
// MultiAgentCoordinator 核心功能
- 任务类型自动匹配
- 智能体状态管理（READY/BUSY/SHUTDOWN）
- 并行任务执行和结果聚合
- 负载均衡和故障恢复
```

**协作流程**：
1. **问题分析**：自动识别查询类型（关系/实体/通用）
2. **任务分解**：将复杂查询分解为专业子任务
3. **并行执行**：多智能体并行处理子任务
4. **结果融合**：智能聚合各智能体的分析结果
5. **结论生成**：基于LLM生成综合性结论

#### 4. **透明化推理过程**
```java
// 完全可观测的智能体工作过程
- AgentResult：详细的执行结果和元数据
- 执行时间统计：性能监控和优化
- 错误处理和降级：保证系统鲁棒性
- 工作流可视化：展示智能体协作过程
```

## 🔄 两种推理方法的协同

### 技术对比

| 特性 | Schema感知推理 | 多智能体协作 |
|------|----------------|--------------|
| **推理深度** | 深度统一推理 | 专业化分析 |
| **处理效率** | 串行优化 | 并行处理 |
| **可解释性** | 推理链路 | 工作过程 |
| **扩展性** | Schema驱动 | 智能体插件 |
| **适用场景** | 复杂知识推理 | 专业化查询 |

### 协同工作模式

```java
// 混合推理架构
1. 多智能体进行专业化分析和数据收集
2. 将分析结果作为上下文输入Schema感知推理
3. 利用reasonWithContext()方法避免重复Schema分析
4. 生成综合性的智能推理结论
```

这种设计体现了现代AI系统的发展趋势：**单一强大模型 + 专业化智能体协作**的混合架构，既保证了推理的深度，又提供了处理的灵活性和透明度。

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
- [TODO](TODO_list.md)

## 演示程序

运行 `GraphTravelerDemo` 体验完整功能：

```bash
cd java-graph-abstraction
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphTravelerDemo"
```

### 功能菜单
```
┌─────────────────────────────────────┐
│        🌐 Graph Traveler Demo       │
│         智能图推理技术展示           │
├─────────────────────────────────────┤
│ 🚀 核心功能                         │
│ 1. 🧠 智能推理查询 (基于Schema)     │
│ 2. 🤖 多智能体协作查询 (NEW!)       │
├─────────────────────────────────────┤
│ 🔧 辅助功能                         │
│ 3. 🔍 实体搜索                      │
│ 4. 📊 Schema分析                    │
│ 5. ⚙️  高级选项...                  │
└─────────────────────────────────────┘
```

### 示例查询
- **关系查询**：`"张机与吴普的关系？"`
- **实体查询**：`"汤液经法是什么相关的医学典籍？"`
- **治疗查询**：`"失眠的治疗方法有哪些？"`
- **多智能体**：自动分工协作处理复杂查询

## 技术亮点

### 🎯 创新特性
- **Schema感知**：首个基于数据库Schema的图推理系统
- **多智能体**：专业化智能体协作，过程完全透明
- **中文优化**：针对中文知识图谱的特殊优化
- **混合架构**：LLM + 专业智能体的协同工作

### 📈 性能优势
- **并行处理**：多智能体并行执行，提升查询效率
- **智能缓存**：Schema信息缓存，避免重复分析
- **深度控制**：可配置推理深度，平衡效果和性能
- **容错机制**：多层降级策略，保证系统稳定性

### 🔬 技术深度
- **多跳推理**：支持最大4跳的深度关系推理
- **路径发现**：BFS算法发现实体间最短连接路径
- **置信度传播**：全链路置信度计算和传播
- **可解释AI**：完整的推理过程记录和可视化

## 开发计划

### 近期目标
- [x] Schema感知智能推理引擎
- [x] 多智能体协作系统
- [x] 中文知识图谱优化
- [ ] 图可视化界面
- [ ] REST API接口

### 长期规划
- [ ] 支持更多图数据库 (ArangoDB, Amazon Neptune)
- [ ] 增强向量搜索能力
- [ ] 分布式推理集群
- [ ] 知识图谱自动构建
- [ ] 多模态知识融合

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
