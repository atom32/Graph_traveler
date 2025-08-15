# 🌐 Graph Traveler 使用指南

## 🚀 快速开始

### 1. 启动程序
```bash
# 赋予执行权限（首次运行）
chmod +x run.sh

# 运行主程序
./run.sh

# 或使用Maven
mvn clean compile exec:java
```

### 2. 初始化数据（可选）
```bash
# 使用统一脚本
./run.sh init

# 或使用Maven
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.DataInitializer"
```

## 📋 主要功能

### 🔍 实体搜索
- **功能**: 根据关键词搜索相关实体
- **适用场景**: 快速查找特定实体
- **示例查询**: 
  - "失眠"
  - "中药"
  - "方剂"

### 🤔 标准推理查询
- **功能**: 基于图结构的逻辑推理
- **适用场景**: 简单的问答查询
- **示例查询**:
  - "什么药物可以治疗失眠？"
  - "哪些方剂包含当归？"

### 🧠 智能推理查询
- **功能**: 基于Schema的智能查询规划
- **适用场景**: 复杂的自然语言问题
- **示例查询**:
  - "我失眠多梦怎么办？"
  - "有什么中药可以补气血？"
  - "治疗感冒的方剂有哪些？"

### 📊 Schema分析
- **功能**: 查看数据库结构信息
- **包含内容**:
  - 节点类型统计
  - 关系类型分析
  - 性能优化建议

## 🔧 配置说明

### 数据库配置
编辑 `src/main/resources/application-local.properties`:

```properties
# 数据库类型 (neo4j/rdf)
graph.database.type=neo4j
graph.database.uri=bolt://localhost:7687
graph.database.username=neo4j
graph.database.password=password

# AI服务配置
openai.api.key=your-api-key
openai.api.url=https://api.openai.com/v1/chat/completions
embedding.api.key=your-embedding-key
embedding.api.url=https://api.openai.com/v1/embeddings
```

### 搜索引擎配置
```properties
# 搜索引擎类型 (simple/advanced)
search.engine.type=advanced

# 推理参数
reasoning.max.depth=3
reasoning.width=3
reasoning.entity.threshold=0.5
reasoning.relation.threshold=0.2
```

## 💡 使用技巧

1. **首次使用**: 建议先运行Schema分析了解数据结构
2. **搜索优化**: 使用具体的关键词获得更好的搜索结果
3. **推理选择**: 
   - 简单问题使用标准推理
   - 复杂自然语言问题使用智能推理
4. **性能优化**: 根据Schema分析的建议创建数据库索引

## 🐛 常见问题

### Q: 搜索结果为空？
A: 
- 检查数据库是否有数据
- 尝试运行DataInitializer初始化测试数据
- 使用不同的关键词

### Q: 推理失败？
A:
- 检查AI服务配置是否正确
- 确认API密钥有效
- 查看系统状态检查各组件状态

### Q: 连接数据库失败？
A:
- 确认Neo4j服务正在运行
- 检查连接配置（URI、用户名、密码）
- 确认网络连接正常

## 📞 技术支持

如有问题，请检查：
1. 日志输出中的错误信息
2. 系统状态检查结果
3. 配置文件是否正确