# Graph Reasoning System Demo Guide

这是一个基于Java的图数据库抽象层和推理系统，灵感来自ToG-2项目。

## 快速开始

### 1. 环境准备

**必需组件：**
- Java 17+
- Maven 3.6+
- Neo4j数据库
- OpenAI API Key

**启动Neo4j：**
```bash
# 使用Docker（推荐）
docker run -p 7474:7474 -p 7687:7687 -e NEO4J_AUTH=neo4j/password neo4j:latest

# 或者本地安装Neo4j并启动
```

**设置API Key：**
```bash
# Linux/Mac
export OPENAI_API_KEY=your_openai_api_key

# Windows
set OPENAI_API_KEY=your_openai_api_key
```

### 2. 运行Demo

**运行脚本：**
```bash
chmod +x run-demo.sh
./run-demo.sh
```

**或者手动运行：**
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphReasoningDemo"
```

### 3. 使用Demo

系统启动后会：
1. 连接到Neo4j数据库
2. 创建示例知识图谱数据
3. 进入交互式问答模式

**示例问题：**
- "Who developed the Theory of Relativity?"
- "Where was Marie Curie born?"
- "What did Isaac Newton formulate?"

## 系统架构

### 核心组件

1. **GraphDatabase接口** - 图数据库抽象层
   - Neo4j实现
   - 支持实体和关系的CRUD操作

2. **SearchEngine接口** - 语义搜索引擎
   - 简单文本相似度实现
   - 支持实体搜索和关系评分

3. **GraphReasoner** - 图推理引擎
   - 多步图探索
   - 基于LLM的答案生成

4. **LLMService接口** - 大语言模型集成
   - OpenAI API实现
   - 支持文本生成

### 推理流程

1. **实体识别** - 从问题中识别相关实体
2. **图探索** - 多步遍历知识图谱
3. **关系评分** - 基于问题对关系进行相关性评分
4. **证据收集** - 收集推理路径和证据
5. **答案生成** - 使用LLM生成最终答案

## 配置说明

### 数据库配置
```properties
graph.database.type=neo4j
graph.database.uri=bolt://localhost:7687
graph.database.username=neo4j
graph.database.password=password
```

### LLM配置
```properties
openai.api.key=${OPENAI_API_KEY:}
openai.model=gpt-3.5-turbo
```

### 推理参数
```properties
reasoning.max.depth=3        # 最大搜索深度
reasoning.width=3           # 每层搜索宽度
reasoning.entity.threshold=0.5    # 实体相关性阈值
reasoning.relation.threshold=0.2  # 关系相关性阈值
```

## 扩展开发

### 添加新的图数据库支持

1. 实现`GraphDatabase`接口
2. 在`GraphReasoningSystem`中添加初始化逻辑

### 添加新的搜索引擎

1. 实现`SearchEngine`接口
2. 集成向量数据库或嵌入模型

### 自定义推理策略

1. 继承或修改`GraphReasoner`
2. 实现自定义的图遍历算法

## 故障排除

### 常见问题

1. **Neo4j连接失败**
   - 检查Neo4j是否启动
   - 验证连接参数

2. **OpenAI API调用失败**
   - 检查API Key是否正确
   - 验证网络连接

3. **编译错误**
   - 确保Java 17+
   - 检查Maven配置

### 日志调试

修改`application.properties`中的日志级别：
```properties
logging.level.com.tog.graph=DEBUG
```

## 性能优化

1. **数据库索引** - 为实体ID和名称创建索引
2. **缓存机制** - 缓存频繁查询的实体和关系
3. **批量操作** - 使用批量查询减少数据库调用
4. **并行处理** - 并行化图探索过程

## 下一步开发

1. 集成真实的嵌入模型（如BGE、Sentence-BERT）
2. 添加更多图数据库支持（如ArangoDB、Amazon Neptune）
3. 实现更复杂的推理策略
4. 添加Web界面
5. 支持大规模知识图谱