# 🚀 Java Graph Abstraction - 快速启动指南

## ✅ 前置条件检查

你已经安装了Maven，很好！现在让我们检查其他必需的组件：

### 1. 检查Java版本
```bash
java -version
# 需要Java 17或更高版本
```

### 2. 检查Maven版本
```bash
mvn -version
# 需要Maven 3.6+
```

## 🎯 快速启动步骤

### 步骤1：启动Neo4j数据库

#### 选项A：使用Docker（推荐）
```bash
# 启动Neo4j容器
docker run -d \
  --name neo4j-graph \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:latest

# 检查是否启动成功
docker ps | grep neo4j
```

#### 选项B：本地安装Neo4j
```bash
# 下载并安装Neo4j Community Edition
# 然后启动服务
neo4j start
```

### 步骤2：设置API密钥

#### Windows:
```cmd
set OPENAI_API_KEY=your_openai_api_key_here
```

#### Linux/Mac:
```bash
export OPENAI_API_KEY=your_openai_api_key_here
```

### 步骤3：编译项目
```bash
# 进入项目目录
cd java-graph-abstraction

# 编译项目
mvn clean compile
```

### 步骤4：运行演示

#### 选项A：使用脚本（Linux/Mac）
```bash
chmod +x run-demo.sh
./run-demo.sh
```

#### 选项B：使用Maven命令（跨平台）
```bash
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphReasoningDemo"
```

#### 选项C：Windows批处理脚本
```cmd
@echo off
echo === Graph Reasoning System Demo ===

if "%OPENAI_API_KEY%"=="" (
    echo Warning: OPENAI_API_KEY environment variable is not set
    echo Please set it with: set OPENAI_API_KEY=your_api_key
    pause
)

echo Building project...
mvn clean compile -q

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Build successful
echo Starting demo...
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphReasoningDemo" -q

pause
```

## 🧪 测试不同功能

### 1. 基础推理演示
```bash
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphReasoningDemo"
```

### 2. 异步推理演示
```bash
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.AsyncGraphReasoningDemo"
```

### 3. 多跳推理演示
```bash
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.MultiHopReasoningDemo"
```

### 4. 并行推理演示
```bash
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.ParallelReasoningDemo"
```

### 5. 嵌入服务测试
```bash
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.EmbeddingTestDemo"
```

## 🎮 使用演示程序

启动后，你会看到交互式界面：

```
=== Interactive Demo ===
Ask questions about the knowledge graph!
Sample questions:
- Who developed the Theory of Relativity?
- Where was Marie Curie born?
- What did Isaac Newton formulate?
Type 'quit' to exit.

Question: 
```

### 示例对话：
```
Question: Who developed the Theory of Relativity?
Reasoning...
==================================================
Question: Who developed the Theory of Relativity?
Answer: Albert Einstein developed the Theory of Relativity.
Reasoning Path:
  1. Depth 0: Albert Einstein -[DEVELOPED]-> Theory of Relativity (score: 0.940)
==================================================

Question: Where was Marie Curie born?
Reasoning...
==================================================
Question: Where was Marie Curie born?
Answer: Marie Curie was born in Poland.
Reasoning Path:
  1. Depth 0: Marie Curie -[BORN_IN]-> Poland (score: 0.900)
==================================================

Question: quit
Demo ended. Goodbye!
```

## 🔧 故障排除

### 问题1：Java版本不兼容
```bash
# 错误信息：Unsupported class file major version
# 解决方案：升级到Java 17+
java -version
```

### 问题2：Neo4j连接失败
```bash
# 错误信息：Cannot connect to Neo4j at localhost:7687
# 解决方案：
docker ps | grep neo4j  # 检查Neo4j是否运行
docker logs neo4j-graph  # 查看Neo4j日志
```

### 问题3：OpenAI API调用失败
```bash
# 错误信息：OpenAI API error
# 解决方案：
echo $OPENAI_API_KEY  # 检查API密钥是否设置
# 或者在Windows: echo %OPENAI_API_KEY%
```

### 问题4：Maven编译失败
```bash
# 清理并重新编译
mvn clean
mvn compile

# 如果还有问题，检查网络连接和Maven仓库
mvn dependency:resolve
```

### 问题5：端口冲突
```bash
# 如果7687端口被占用
netstat -an | grep 7687  # 检查端口占用
# 修改Neo4j端口或停止占用端口的服务
```

## 📊 性能监控

### 查看系统资源使用
```bash
# 查看Java进程
jps -v

# 查看内存使用
jstat -gc [pid]

# 查看Neo4j状态
docker exec neo4j-graph cypher-shell -u neo4j -p password "CALL dbms.queryJmx('org.neo4j:instance=kernel#0,name=Store file sizes')"
```

## 🎯 下一步

### 1. 自定义配置
- 修改 `src/main/resources/application.properties`
- 或者修改 `src/main/java/com/tog/graph/config/GraphConfig.java`

### 2. 添加自己的数据
- 修改 `GraphReasoningDemo.java` 中的 `createSampleData` 方法
- 或者直接在Neo4j中导入你的数据

### 3. 集成到你的项目
```java
// 作为库使用
GraphConfig config = new GraphConfig();
config.setOpenaiApiKey("your-api-key");
GraphReasoningSystem system = new GraphReasoningSystem(config);

ReasoningResult result = system.reason("你的问题");
System.out.println(result.getAnswer());
```

### 4. 部署为Web服务
- 参考 `SERVICE_DEPLOYMENT_GUIDE.md`
- 添加Spring Boot Web依赖
- 创建REST API控制器

## 🎉 成功标志

如果你看到以下输出，说明系统运行成功：

```
=== Graph Reasoning System Demo ===
Neo4j connection OK
Build successful
Initializing system...
Graph database connected: neo4j
Embedding service initialized: openai (dimension: 1536)
Search engine initialized: simple
LLM service initialized: gpt-3.5-turbo
Graph reasoner initialized with depth=3, width=5
Sample data created successfully!

=== Interactive Demo ===
Ask questions about the knowledge graph!
```

现在你可以开始使用这个强大的图推理系统了！🚀