# 🚀 Java Graph Abstraction - 服务部署指南

## 📋 当前状态分析

### 🔍 现有服务方式
目前这个项目主要提供以下几种使用方式：

1. **命令行演示程序** (主要方式)
   - `GraphReasoningDemo.java` - 交互式命令行问答
   - `AsyncGraphReasoningDemo.java` - 异步推理演示
   - `MultiHopReasoningDemo.java` - 多跳推理演示

2. **编程API** (库的形式)
   - `GraphReasoningSystem` - 核心推理系统类
   - 可以作为Java库集成到其他项目中

3. **目前缺少的服务方式**
   - ❌ REST API服务
   - ❌ Web界面
   - ❌ gRPC服务
   - ❌ 消息队列服务

## 🎯 如何将其转换为Web服务

### 方法1：添加Spring Boot Web支持（推荐）

#### 1. 修改 `pom.xml` 添加Web依赖
```xml
<dependencies>
    <!-- 现有依赖保持不变 -->
    
    <!-- 添加Spring Boot Web支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.2.0</version>
    </dependency>
    
    <!-- 添加Spring Boot Actuator用于健康检查 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
        <version>3.2.0</version>
    </dependency>
</dependencies>
```

#### 2. 创建REST API控制器
```java
// src/main/java/com/tog/graph/web/GraphReasoningController.java
@RestController
@RequestMapping("/api/v1/reasoning")
@CrossOrigin(origins = "*")
public class GraphReasoningController {
    
    private final GraphReasoningSystem reasoningSystem;
    
    public GraphReasoningController() {
        // 初始化推理系统
        GraphConfig config = createConfig();
        this.reasoningSystem = new GraphReasoningSystem(config);
    }
    
    @PostMapping("/question")
    public ResponseEntity<ReasoningResponse> askQuestion(@RequestBody QuestionRequest request) {
        try {
            ReasoningResult result = reasoningSystem.reason(request.getQuestion());
            
            ReasoningResponse response = new ReasoningResponse(
                request.getQuestion(),
                result.getAnswer(),
                result.getEvidences(),
                "success"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ReasoningResponse errorResponse = new ReasoningResponse(
                request.getQuestion(),
                null,
                Collections.emptyList(),
                "error: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(errorResponse);
        }
    }
    
    @PostMapping("/question/async")
    public CompletableFuture<ResponseEntity<ReasoningResponse>> askQuestionAsync(
            @RequestBody QuestionRequest request) {
        
        return reasoningSystem.reasonAsync(request.getQuestion())
                .thenApply(result -> {
                    ReasoningResponse response = new ReasoningResponse(
                        request.getQuestion(),
                        result.getAnswer(),
                        result.getEvidences(),
                        "success"
                    );
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    ReasoningResponse errorResponse = new ReasoningResponse(
                        request.getQuestion(),
                        null,
                        Collections.emptyList(),
                        "error: " + throwable.getMessage()
                    );
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(errorResponse);
                });
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "healthy");
        status.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(status);
    }
}
```

#### 3. 创建数据传输对象
```java
// src/main/java/com/tog/graph/web/dto/QuestionRequest.java
public class QuestionRequest {
    private String question;
    private Map<String, Object> options;
    
    // constructors, getters, setters
}

// src/main/java/com/tog/graph/web/dto/ReasoningResponse.java
public class ReasoningResponse {
    private String question;
    private String answer;
    private List<String> evidences;
    private String status;
    private long timestamp;
    
    // constructors, getters, setters
}
```

#### 4. 创建Spring Boot主类
```java
// src/main/java/com/tog/graph/GraphReasoningApplication.java
@SpringBootApplication
public class GraphReasoningApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GraphReasoningApplication.class, args);
    }
}
```

#### 5. 配置文件
```yaml
# src/main/resources/application.yml
server:
  port: 8080
  servlet:
    context-path: /graph-reasoning

spring:
  application:
    name: graph-reasoning-service

# 现有配置保持不变
graph:
  database:
    type: neo4j
    uri: bolt://localhost:7687
    username: neo4j
    password: password

openai:
  api:
    key: ${OPENAI_API_KEY:}
    url: https://api.openai.com/v1/chat/completions
  model: gpt-3.5-turbo

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

### 方法2：使用当前的库形式集成

#### 在你的Web应用中集成
```java
// 在你的Spring Boot应用中
@Service
public class GraphReasoningService {
    
    private final GraphReasoningSystem reasoningSystem;
    
    @PostConstruct
    public void init() {
        GraphConfig config = new GraphConfig();
        // 配置设置...
        this.reasoningSystem = new GraphReasoningSystem(config);
    }
    
    public CompletableFuture<String> askQuestion(String question) {
        return reasoningSystem.reasonAsync(question)
                .thenApply(ReasoningResult::getAnswer);
    }
}
```

### 方法3：Docker容器化部署

#### 创建 Dockerfile
```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制项目文件
COPY target/graph-abstraction-1.0.0.jar app.jar
COPY src/main/resources/application.properties application.properties

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV JAVA_OPTS="-Xmx2g -Xms1g"

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 创建 docker-compose.yml
```yaml
version: '3.8'

services:
  graph-reasoning:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - NEO4J_URI=bolt://neo4j:7687
      - NEO4J_USERNAME=neo4j
      - NEO4J_PASSWORD=password
    depends_on:
      - neo4j
    networks:
      - graph-network

  neo4j:
    image: neo4j:5.15
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      - NEO4J_AUTH=neo4j/password
      - NEO4J_PLUGINS=["apoc"]
    volumes:
      - neo4j_data:/data
    networks:
      - graph-network

volumes:
  neo4j_data:

networks:
  graph-network:
    driver: bridge
```

## 🔧 快速启动Web服务

### 1. 添加Web依赖并重新编译
```bash
# 修改pom.xml后重新编译
mvn clean compile
```

### 2. 启动服务
```bash
# 方式1：直接运行Spring Boot应用
mvn spring-boot:run

# 方式2：打包后运行
mvn clean package
java -jar target/graph-abstraction-1.0.0.jar

# 方式3：使用Docker
docker-compose up -d
```

### 3. 测试API
```bash
# 健康检查
curl http://localhost:8080/graph-reasoning/api/v1/reasoning/health

# 同步问答
curl -X POST http://localhost:8080/graph-reasoning/api/v1/reasoning/question \
  -H "Content-Type: application/json" \
  -d '{"question": "Who developed the Theory of Relativity?"}'

# 异步问答
curl -X POST http://localhost:8080/graph-reasoning/api/v1/reasoning/question/async \
  -H "Content-Type: application/json" \
  -d '{"question": "Where was Marie Curie born?"}'
```

## 📊 API响应示例

### 成功响应
```json
{
  "question": "Who developed the Theory of Relativity?",
  "answer": "Albert Einstein developed the Theory of Relativity.",
  "evidences": [
    "Albert Einstein -[DEVELOPED]-> Theory of Relativity (score: 0.940)"
  ],
  "status": "success",
  "timestamp": 1704067200000
}
```

### 错误响应
```json
{
  "question": "Invalid question",
  "answer": null,
  "evidences": [],
  "status": "error: Reasoning failed",
  "timestamp": 1704067200000
}
```

## 🎯 部署选项总结

| 部署方式 | 优点 | 缺点 | 适用场景 |
|---------|------|------|----------|
| **命令行程序** | 简单直接 | 不能远程访问 | 本地测试、演示 |
| **REST API服务** | 标准化、易集成 | 需要额外开发 | 生产环境、微服务 |
| **Java库集成** | 性能最好 | 语言限制 | Java项目集成 |
| **Docker容器** | 易部署、隔离性好 | 资源开销 | 云部署、容器化环境 |

## 🚀 推荐部署方案

**对于生产环境**，推荐使用 **REST API + Docker** 的方式：
1. 提供标准的HTTP API接口
2. 支持异步处理
3. 容器化部署，易于扩展
4. 包含健康检查和监控

**对于开发测试**，可以继续使用现有的命令行演示程序。

你希望我帮你实现哪种服务方式？