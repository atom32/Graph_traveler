# 🚀 Graph Traveler 快速启动指南

## 前提条件

- Java 17 或更高版本
- Maven 3.6 或更高版本
- Neo4j 数据库（可选，用于完整功能）

## 选择运行方式

Graph Traveler 现在支持两种运行方式：

### 🌐 方式一：REST API 服务（推荐用于集成开发）

**Windows 用户：**
```cmd
start-api.bat
```

**Linux/Mac 用户：**
```bash
chmod +x start-api.sh
./start-api.sh
```

**或者直接使用 Maven：**
```bash
mvn spring-boot:run
```

### 🖥️ 方式二：命令行演示程序（推荐用于学习和测试）

**Windows 用户：**
```cmd
run-demo.bat
```

**Linux/Mac 用户：**
```bash
chmod +x run-demo.sh
./run-demo.sh
```

**或者使用 Maven：**
```bash
mvn exec:java
```

## API 服务验证步骤

### API 服务验证

如果你选择了 API 服务方式，打开浏览器访问：http://localhost:8080/api/v1/graph/health

如果看到类似以下的 JSON 响应，说明服务启动成功：
```json
{
  "status": "healthy",
  "databaseConnected": true,
  "searchEngineReady": true,
  "reasonerReady": true,
  "schemaAwareReasonerReady": true
}
```

### 命令行演示验证

如果你选择了命令行方式，程序会显示一个交互式菜单，你可以：
1. 选择不同的功能进行测试
2. 输入查询问题
3. 查看推理结果

### API 测试方式

#### 方式一：使用测试客户端
打开 `test-api.html` 文件，在浏览器中进行交互式测试。

#### 方式二：使用 cURL 命令

**实体搜索：**
```bash
curl -X POST http://localhost:8080/api/v1/graph/search/entities \
  -H "Content-Type: application/json" \
  -d '{"query": "张仲景", "limit": 5}'
```

**智能推理查询：**
```bash
curl -X POST http://localhost:8080/api/v1/graph/reasoning/schema-aware \
  -H "Content-Type: application/json" \
  -d '{"question": "张仲景与伤寒论的关系？"}'
```

**多智能体协作：**
```bash
curl -X POST http://localhost:8080/api/v1/graph/agents/collaborative-query \
  -H "Content-Type: application/json" \
  -d '{"query": "分析张机与王叔和的关系"}'
```

## 常见问题

### Q: 服务启动失败怎么办？
A: 检查以下几点：
1. 确保 Java 17+ 已安装：`java -version`
2. 确保 Maven 已安装：`mvn -version`
3. 检查端口 8080 是否被占用
4. 查看控制台错误信息

### Q: 数据库连接失败？
A: 
1. 如果没有 Neo4j，服务仍可运行，但功能有限
2. 要使用完整功能，请安装并启动 Neo4j
3. 在 `application.yml` 中配置数据库连接信息

### Q: API 返回错误？
A: 
1. 检查请求格式是否正确
2. 查看服务日志了解详细错误信息
3. 确保服务状态为 healthy

## 下一步

- 查看完整 API 文档：`API_DOCUMENTATION.md`
- 了解项目架构：`README.md`
- 配置数据库连接：编辑 `src/main/resources/application.yml`

## 支持

如果遇到问题，请检查：
1. 控制台日志输出
2. `logs/graph-traveler-api.log` 文件
3. 服务健康检查端点：`/api/v1/graph/health`