# 配置说明

## 🔐 安全配置

为了保护API密钥等敏感信息，本项目使用以下安全措施：

### 1. 配置文件设置

1. **复制模板文件**：
   ```bash
   cp src/main/resources/application.properties.template src/main/resources/application.properties
   ```

2. **编辑配置文件**：
   ```bash
   # 编辑 src/main/resources/application.properties
   # 替换以下占位符为实际值：
   
   openai.api.key=your-actual-openai-api-key
   embedding.api.key=your-actual-embedding-api-key
   ```

### 2. 支持的API服务

#### OpenAI 官方API
```properties
openai.api.url=https://api.openai.com/v1/chat/completions
openai.model=gpt-3.5-turbo
embedding.api.url=https://api.openai.com/v1/embeddings
embedding.model=text-embedding-ada-002
```

#### 自定义API服务
```properties
# 示例：本地部署的模型服务
openai.api.url=http://localhost:3002/v1/chat/completions
openai.model=Qwen3-235B

# 示例：SiliconFlow API
embedding.api.url=https://api.siliconflow.cn/v1/embeddings
embedding.model=BAAI/bge-large-zh-v1.5
```

### 3. 环境变量支持

你也可以使用环境变量来设置敏感信息：

```bash
export OPENAI_API_KEY="your-api-key"
export EMBEDDING_API_KEY="your-embedding-key"
```

然后在配置文件中使用：
```properties
openai.api.key=${OPENAI_API_KEY}
embedding.api.key=${EMBEDDING_API_KEY}
```

### 4. 不同环境的配置

- `application.properties` - 默认配置
- `application-dev.properties` - 开发环境
- `application-prod.properties` - 生产环境
- `application-local.properties` - 本地环境

使用方式：
```bash
# 指定环境
java -Dspring.profiles.active=dev -jar your-app.jar
```

## ⚠️ 安全注意事项

1. **永远不要提交包含真实API密钥的配置文件**
2. **使用 .gitignore 忽略敏感配置文件**
3. **定期轮换API密钥**
4. **在生产环境中使用环境变量或密钥管理服务**

## 🔧 配置验证

运行程序时，系统会验证配置：

```
✅ OpenAI API Key: ***Cd93
✅ OpenAI API URL: http://10.8.8.77:3002/v1/chat/completions
✅ Embedding API Key: ***neon
✅ Embedding API URL: https://api.siliconflow.cn/v1/embeddings
```

如果配置有问题，系统会显示相应的错误信息。