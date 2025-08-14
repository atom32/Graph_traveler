# 🔧 LLM供应商配置修改指南

## 📍 配置修改位置

### 方法1：修改配置文件（推荐）

#### 1. 修改 `src/main/resources/application.properties`
```properties
# Graph Database Configuration
graph.database.type=neo4j
graph.database.uri=bolt://localhost:7687
graph.database.username=neo4j
graph.database.password=password

# LLM Configuration - 修改这里！
openai.api.key=${YOUR_API_KEY:}
openai.api.url=https://your-llm-provider.com/v1/chat/completions
openai.model=your-model-name

# Embedding Configuration - 如果需要修改embedding服务
embedding.api.url=https://your-embedding-provider.com/v1/embeddings
embedding.model=your-embedding-model

# Search Engine Configuration
search.engine.type=simple

# Reasoning Configuration
reasoning.max.depth=3
reasoning.width=3
reasoning.entity.threshold=0.5
reasoning.relation.threshold=0.2
reasoning.temperature=0.0
reasoning.max.tokens=256

# Logging Configuration
logging.level.com.tog.graph=INFO
logging.level.org.neo4j=WARN
```

#### 2. 修改 `src/main/java/com/tog/graph/config/GraphConfig.java`
```java
public class GraphConfig {
    // ... 其他配置 ...
    
    // LLM配置 - 修改默认值
    private String openaiApiKey;
    private String openaiApiUrl = "https://your-llm-provider.com/v1/chat/completions";  // 修改这里
    private String openaiModel = "your-model-name";  // 修改这里
    
    // 嵌入服务配置 - 如果需要修改
    private String embeddingServiceType = "openai";
    private String embeddingApiUrl = "https://your-embedding-provider.com/v1/embeddings";  // 修改这里
    private String embeddingModel = "your-embedding-model";  // 修改这里
    private int embeddingCacheSize = 1000;
    
    // ... getter和setter方法保持不变 ...
}
```

### 方法2：在演示程序中直接修改

#### 修改 `src/main/java/com/tog/graph/demo/GraphReasoningDemo.java`
```java
private static GraphConfig createConfig() {
    GraphConfig config = new GraphConfig();
    
    // 从环境变量获取API密钥
    String apiKey = System.getenv("YOUR_API_KEY_ENV_VAR");  // 修改环境变量名
    if (apiKey != null) {
        config.setOpenaiApiKey(apiKey);
    }
    
    // 直接设置LLM配置
    config.setOpenaiApiUrl("https://your-llm-provider.com/v1/chat/completions");  // 修改API URL
    config.setOpenaiModel("your-model-name");  // 修改模型名称
    
    // 如果需要修改embedding配置
    config.setEmbeddingApiUrl("https://your-embedding-provider.com/v1/embeddings");
    config.setEmbeddingModel("your-embedding-model");
    
    // Neo4j配置（可以根据需要修改）
    config.setUri("bolt://localhost:7687");
    config.setUsername("neo4j");
    config.setPassword("password");
    
    return config;
}
```

### 方法3：通过环境变量（最灵活）

#### 设置环境变量
```bash
# Linux/Mac
export YOUR_API_KEY=your_actual_api_key
export LLM_BASE_URL=https://your-llm-provider.com/v1/chat/completions
export LLM_MODEL=your-model-name
export EMBEDDING_BASE_URL=https://your-embedding-provider.com/v1/embeddings
export EMBEDDING_MODEL=your-embedding-model

# Windows
set YOUR_API_KEY=your_actual_api_key
set LLM_BASE_URL=https://your-llm-provider.com/v1/chat/completions
set LLM_MODEL=your-model-name
set EMBEDDING_BASE_URL=https://your-embedding-provider.com/v1/embeddings
set EMBEDDING_MODEL=your-embedding-model
```

#### 修改代码读取环境变量
```java
private static GraphConfig createConfig() {
    GraphConfig config = new GraphConfig();
    
    // 从环境变量读取配置
    String apiKey = System.getenv("YOUR_API_KEY");
    String baseUrl = System.getenv("LLM_BASE_URL");
    String model = System.getenv("LLM_MODEL");
    String embeddingUrl = System.getenv("EMBEDDING_BASE_URL");
    String embeddingModel = System.getenv("EMBEDDING_MODEL");
    
    if (apiKey != null) config.setOpenaiApiKey(apiKey);
    if (baseUrl != null) config.setOpenaiApiUrl(baseUrl);
    if (model != null) config.setOpenaiModel(model);
    if (embeddingUrl != null) config.setEmbeddingApiUrl(embeddingUrl);
    if (embeddingModel != null) config.setEmbeddingModel(embeddingModel);
    
    return config;
}
```

## 🎯 常见LLM供应商配置示例

### 1. 使用OpenAI兼容API（如vLLM、Ollama等）
```properties
openai.api.url=http://localhost:8000/v1/chat/completions
openai.model=llama-3.1-8b-instruct
```

### 2. 使用Azure OpenAI
```properties
openai.api.url=https://your-resource.openai.azure.com/openai/deployments/your-deployment/chat/completions?api-version=2024-02-15-preview
openai.model=gpt-4
```

### 3. 使用Anthropic Claude（需要适配器）
```properties
openai.api.url=https://api.anthropic.com/v1/messages
openai.model=claude-3-sonnet-20240229
```

### 4. 使用Google Gemini（需要适配器）
```properties
openai.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
openai.model=gemini-pro
```

### 5. 使用本地部署的模型
```properties
openai.api.url=http://localhost:11434/v1/chat/completions
openai.model=llama3.1:8b
```

## 🔧 高级配置选项

### 推理参数调整
```properties
# 推理深度（图遍历的最大层数）
reasoning.max.depth=3

# 搜索宽度（每层探索的实体数量）
reasoning.width=5

# 实体相似度阈值
reasoning.entity.threshold=0.3

# 关系相似度阈值
reasoning.relation.threshold=0.2

# LLM生成参数
reasoning.temperature=0.0
reasoning.max.tokens=512
```

### 性能优化配置
```java
// 在GraphConfig中添加
private int threadPoolSize = 4;           // 并行处理线程数
private int batchSize = 10;               // 批处理大小
private boolean enableCaching = true;     // 启用缓存
private int cacheSize = 1000;             // 缓存大小
private long requestTimeout = 30000;      // 请求超时时间(ms)
```

## 🚀 快速配置脚本

创建一个配置脚本 `configure-llm.sh`：
```bash
#!/bin/bash

echo "=== LLM Provider Configuration ==="

# 读取用户输入
read -p "Enter your API key: " API_KEY
read -p "Enter base URL (e.g., https://api.openai.com/v1/chat/completions): " BASE_URL
read -p "Enter model name (e.g., gpt-3.5-turbo): " MODEL_NAME

# 设置环境变量
export YOUR_API_KEY="$API_KEY"
export LLM_BASE_URL="$BASE_URL"
export LLM_MODEL="$MODEL_NAME"

echo "Configuration set!"
echo "API Key: $YOUR_API_KEY"
echo "Base URL: $LLM_BASE_URL"
echo "Model: $LLM_MODEL"

# 运行演示
echo "Starting demo..."
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphReasoningDemo"
```

## 📝 注意事项

### 1. API兼容性
- 确保你的LLM供应商提供OpenAI兼容的API格式
- 如果不兼容，可能需要修改 `OpenAIService.java` 中的请求格式

### 2. 认证方式
- 不同供应商可能使用不同的认证方式（Bearer token、API key等）
- 可能需要修改HTTP请求头的设置

### 3. 响应格式
- 确保响应格式与OpenAI API兼容
- 特别注意 `choices[0].message.content` 字段的路径

### 4. 错误处理
- 不同供应商的错误码和错误信息格式可能不同
- 建议测试错误处理逻辑

## 🧪 测试配置

修改配置后，运行测试：
```bash
# 测试基本功能
./run-demo.sh

# 测试异步功能
./run-async-demo.sh

# 测试embedding功能
./test-embedding.sh
```

这样你就可以轻松地切换到任何LLM供应商了！