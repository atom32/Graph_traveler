# Graph Traveler REST API 文档

## 概述

Graph Traveler API 是一个基于 Spring Boot 的 REST API 服务，提供图推理和知识发现功能。

## 启动服务

### 方式一：使用 Maven
```bash
cd java-graph-abstraction
mvn spring-boot:run
```

### 方式二：打包后运行
```bash
cd java-graph-abstraction
mvn clean package
java -jar target/graph-abstraction-1.0.0.jar
```

服务默认运行在 `http://localhost:8080`

## API 接口

### 1. 健康检查

**GET** `/api/v1/graph/health`

检查服务状态和各组件健康状况。

**响应示例：**
```json
{
  "status": "healthy",
  "databaseConnected": true,
  "searchEngineReady": true,
  "reasonerReady": true,
  "schemaAwareReasonerReady": true
}
```

### 2. 实体搜索

**POST** `/api/v1/graph/search/entities`

搜索图数据库中的实体。

**请求体：**
```json
{
  "query": "张仲景",
  "limit": 10
}
```

**响应示例：**
```json
{
  "success": true,
  "entities": [
    {
      "id": "123",
      "name": "张仲景",
      "type": "Person",
      "properties": {
        "dynasty": "东汉",
        "profession": "医学家"
      },
      "score": 0.95
    }
  ],
  "totalFound": 1,
  "executionTime": 150
}
```

### 3. 智能推理查询

**POST** `/api/v1/graph/reasoning/schema-aware`

使用 Schema 感知的智能推理引擎进行查询。

**请求体：**
```json
{
  "question": "张仲景与伤寒论的关系是什么？"
}
```

**响应示例：**
```json
{
  "success": true,
  "answer": "张仲景是《伤寒论》的作者，这是一部重要的中医经典著作...",
  "confidence": 0.92,
  "reasoningSteps": [
    "识别实体：张仲景、伤寒论",
    "搜索相关关系",
    "分析连接路径",
    "生成答案"
  ],
  "sourceEntities": ["张仲景", "伤寒论"],
  "executionTime": 2500,
  "reasoningType": "schema-aware"
}
```

### 4. 标准推理查询

**POST** `/api/v1/graph/reasoning/standard`

使用标准推理引擎进行查询。

**请求体：**
```json
{
  "question": "失眠的治疗方法有哪些？"
}
```

### 5. 多智能体协作查询

**POST** `/api/v1/graph/agents/collaborative-query`

使用多智能体系统进行协作查询。

**请求体：**
```json
{
  "query": "分析张机与王叔和的关系",
  "context": {
    "limit": 20,
    "searchLimit": 15,
    "maxDepth": 3,
    "includeIndirect": true
  }
}
```

**上下文参数说明：**
- `limit`: 通用搜索结果数量限制（默认：20）
- `searchLimit`: 实体搜索结果数量限制（默认：15）
- `maxDepth`: 关系分析最大深度（默认：3）
- `includeIndirect`: 是否包含间接关系（默认：true）

**响应示例：**
```json
{
  "success": true,
  "result": "=== 张机 的关系分析 ===\n张机（张仲景）是东汉末年著名医学家...\n\n=== 王叔和 的关系分析 ===\n王叔和是西晋时期的医学家...",
  "metadata": {
    "analyzed_entities_count": 5,
    "total_found_entities": 15,
    "张机_relationships": 8,
    "王叔和_relationships": 6,
    "agentsUsed": ["EntitySearchAgent", "RelationshipAnalysisAgent"]
  },
  "executionTime": 3200
}
```

### 6. 单个智能体任务执行

**POST** `/api/v1/graph/agents/{agentType}/execute`

执行特定智能体的任务。

支持的智能体类型：
- `entity_search` - 实体搜索智能体
- `relationship_analysis` - 关系分析智能体

**请求体：**
```json
{
  "task": "搜索中医相关实体",
  "context": {
    "limit": 5,
    "threshold": 0.3
  }
}
```

### 7. Schema 信息

**GET** `/api/v1/graph/schema`

获取图数据库的 Schema 信息。

**响应示例：**
```json
{
  "available": true,
  "summary": "数据库包含 5 种节点类型和 8 种关系类型",
  "nodeTypes": ["Person", "Book", "Medicine", "Disease", "Treatment"],
  "relationshipTypes": ["WROTE", "TREATS", "CAUSES", "RELATED_TO"],
  "totalNodes": 1250,
  "totalRelationships": 3400
}
```

## 错误处理

所有 API 在出错时会返回相应的 HTTP 状态码和错误信息：

```json
{
  "success": false,
  "error": "详细的错误描述信息"
}
```

常见状态码：
- `200` - 成功
- `400` - 请求参数错误
- `500` - 服务器内部错误
- `503` - 服务不可用

## 配置

### 数据库配置

在 `application.yml` 中配置数据库连接：

```yaml
graph:
  database:
    type: neo4j
    neo4j:
      uri: bolt://localhost:7687
      username: neo4j
      password: your_password
```

### LLM 配置

配置大语言模型服务：

```yaml
graph:
  reasoning:
    llm:
      provider: openai
      api-key: ${OPENAI_API_KEY}
      model: gpt-3.5-turbo
```

## 使用示例

### cURL 示例

```bash
# 健康检查
curl -X GET http://localhost:8080/api/v1/graph/health

# 实体搜索
curl -X POST http://localhost:8080/api/v1/graph/search/entities \
  -H "Content-Type: application/json" \
  -d '{"query": "张仲景", "limit": 5}'

# 智能推理查询
curl -X POST http://localhost:8080/api/v1/graph/reasoning/schema-aware \
  -H "Content-Type: application/json" \
  -d '{"question": "张仲景与伤寒论的关系？"}'
```

### JavaScript 示例

```javascript
// 实体搜索
const searchEntities = async (query) => {
  const response = await fetch('http://localhost:8080/api/v1/graph/search/entities', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      query: query,
      limit: 10
    })
  });
  
  return await response.json();
};

// 智能推理查询
const performReasoning = async (question) => {
  const response = await fetch('http://localhost:8080/api/v1/graph/reasoning/schema-aware', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      question: question
    })
  });
  
  return await response.json();
};
```

## 性能优化建议

1. **连接池配置**：合理配置数据库连接池大小
2. **缓存策略**：对频繁查询的结果进行缓存
3. **异步处理**：对于耗时的推理任务，考虑使用异步处理
4. **限流控制**：在生产环境中添加 API 限流机制

## 监控和日志

- 应用日志位置：`logs/graph-traveler-api.log`
- 健康检查端点：`/api/v1/graph/health`
- 可集成 Spring Boot Actuator 进行更详细的监控