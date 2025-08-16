package com.tog.graph.reasoning;

import com.tog.graph.core.GraphDatabase;
import com.tog.graph.core.Entity;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.AdvancedGraphSearchEngine;
import com.tog.graph.llm.LLMService;
import com.tog.graph.schema.GraphSchema;
import com.tog.graph.schema.NodeTypeInfo;
import com.tog.graph.schema.RelationshipTypeInfo;
import com.tog.graph.schema.SearchStrategy;
import com.tog.graph.prompt.PromptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于Schema感知的图推理器
 * 利用Schema信息指导LLM进行智能的实体抽取和查询规划
 */
public class SchemaAwareGraphReasoner {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaAwareGraphReasoner.class);
    
    private final GraphDatabase graphDatabase;
    private final SearchEngine searchEngine;
    private final LLMService llmService;
    private final ReasoningConfig config;
    private final GraphReasoner fallbackReasoner;
    private final PromptManager promptManager;
    
    public SchemaAwareGraphReasoner(GraphDatabase graphDatabase, SearchEngine searchEngine, 
                                   LLMService llmService, ReasoningConfig config) {
        this.graphDatabase = graphDatabase;
        this.searchEngine = searchEngine;
        this.llmService = llmService;
        this.config = config;
        this.fallbackReasoner = new GraphReasoner(graphDatabase, searchEngine, llmService, config);
        this.promptManager = PromptManager.getInstance();
    }
    
    /**
     * 基于Schema的智能推理
     */
    public ReasoningResult reason(String question) {
        try {
            logger.info("Starting schema-aware reasoning for: {}", question);
            
            // 1. 获取Schema信息
            GraphSchema schema = getGraphSchema();
            if (schema == null) {
                logger.warn("No schema available, falling back to standard reasoning");
                return fallbackReasoner.reason(question);
            }
            
            // 2. 使用LLM和Schema进行智能实体抽取
            EntityExtractionResult extraction = performSchemaGuidedEntityExtraction(question, schema);
            
            if (extraction.getExtractedEntities().isEmpty()) {
                logger.warn("No entities extracted, falling back to standard reasoning");
                return fallbackReasoner.reason(question);
            }
            
            // 3. 基于Schema规划查询策略
            QueryPlan queryPlan = planSchemaAwareQuery(question, extraction, schema);
            
            // 4. 执行查询计划
            ReasoningResult result = executeQueryPlan(question, queryPlan, schema);
            
            logger.info("Schema-aware reasoning completed successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("Schema-aware reasoning failed, falling back", e);
            return fallbackReasoner.reason(question);
        }
    }
    
    /**
     * 获取图Schema
     */
    private GraphSchema getGraphSchema() {
        if (searchEngine instanceof AdvancedGraphSearchEngine) {
            return ((AdvancedGraphSearchEngine) searchEngine).getSchema();
        }
        return null;
    }
    
    /**
     * 基于Schema指导的实体抽取
     */
    private EntityExtractionResult performSchemaGuidedEntityExtraction(String question, GraphSchema schema) {
        // 使用PromptManager获取实体抽取模板
        String schemaContext = buildSchemaContext(schema);
        String prompt = promptManager.getPrompt("entity-extraction", 
                PromptManager.params("schema_context", schemaContext, "question", question));
        
        try {
            String llmResponse = llmService.generate(prompt, 0.1, 512);
            return parseEntityExtractionResponse(llmResponse, question);
            
        } catch (Exception e) {
            logger.error("LLM entity extraction failed", e);
            return new EntityExtractionResult(); // 返回空结果
        }
    }
    
    /**
     * 构建Schema上下文描述
     */
    private String buildSchemaContext(GraphSchema schema) {
        StringBuilder context = new StringBuilder();
        
        context.append("节点类型:\n");
        for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
            context.append(String.format("- %s (%d个节点)\n", nodeType.getLabel(), nodeType.getCount()));
            
            // 添加主要属性
            List<String> mainProperties = nodeType.getSearchableProperties().stream()
                    .limit(3)
                    .map(prop -> prop.getName())
                    .collect(Collectors.toList());
            
            if (!mainProperties.isEmpty()) {
                context.append(String.format("  主要属性: %s\n", String.join(", ", mainProperties)));
            }
        }
        
        context.append("\n关系类型:\n");
        for (RelationshipTypeInfo relType : schema.getRelationshipTypes()) {
            context.append(String.format("- %s (%d个关系)\n", relType.getType(), relType.getTotalCount()));
            
            // 添加主要连接模式
            var topPatterns = relType.getMostCommonPatterns(2);
            for (var pattern : topPatterns) {
                context.append(String.format("  %s -> %s\n", 
                             pattern.getSourceLabel(), pattern.getTargetLabel()));
            }
        }
        
        return context.toString();
    }
    
    /**
     * 解析LLM的实体抽取响应
     */
    private EntityExtractionResult parseEntityExtractionResponse(String response, String question) {
        EntityExtractionResult result = new EntityExtractionResult();
        
        try {
            logger.debug("Parsing LLM response: {}", response);
            
            // 智能实体抽取 - 结合LLM响应和问题分析
            logger.debug("LLM Response: {}", response);
            logger.debug("Original Question: {}", question);
            
            // 智能实体抽取 - 结合多种策略
            
            // 1. 尝试解析LLM的JSON响应
            List<String> llmEntities = extractEntitiesFromLLMResponse(response);
            for (String entity : llmEntities) {
                result.addEntity(entity, "ANY", 0.9, Arrays.asList("name", "全称", "title"));
                logger.debug("Added LLM extracted entity: {}", entity);
            }
            
            // 2. 基于问题的实体识别
            List<String> questionEntities = extractEntitiesFromQuestion(question);
            for (String entity : questionEntities) {
                // 根据实体特征推测类型
                String entityType = inferEntityType(entity, schema);
                double confidence = calculateEntityConfidence(entity, question);
                
                result.addEntity(entity, entityType, confidence, 
                    getOptimalSearchProperties(entity, entityType, schema));
                logger.debug("Added question entity: {} (type: {}, confidence: {:.3f})", 
                           entity, entityType, confidence);
            }
            
            // 3. 设置查询意图
            String intent = inferQueryIntent(question);
            result.setQueryIntent(intent);
            logger.debug("Inferred query intent: {}", intent);
            
            // 添加相关的关系类型 这里是硬编码，不好
            result.addRelationship("主治", 0.8);
            result.addRelationship("组成", 0.8);
            result.addRelationship("治疗方法", 0.7);
            result.addRelationship("因果关系", 0.7);
            
            // 设置查询意图
            result.setQueryIntent("查找汤液经法相关的医学典籍信息");
            
        } catch (Exception e) {
            logger.error("Failed to parse entity extraction response", e);
        }
        
        return result;
    }
    
    /**
     * 提取中文人名
     */
    private String[] extractChineseNames(String text) {
        List<String> names = new ArrayList<>();
        
        // 简单的中文人名识别规则
        String[] words = text.split("[\\s，。！？、的和与]+");
        
        for (String word : words) {
            // 中文人名通常是2-4个字，且不包含常见的非人名词汇
            if (word.length() >= 2 && word.length() <= 4 && 
                word.matches("[\u4e00-\u9fa5]+") && // 只包含中文字符
                !isCommonWord(word) &&
                !word.matches(".*[关系什么怎么样哪里为什么].*")) {
                
                names.add(word);
            }
        }
        
        return names.toArray(new String[0]);
    }
    
    /**
     * 判断是否为常见词汇 这部分应该有词典
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {
            "关系", "什么", "怎么", "哪里", "为什么", "怎样", "如何", 
            "这个", "那个", "这些", "那些", "可以", "应该", "需要",
            "问题", "方法", "时候", "地方", "东西", "事情", "情况"
        };
        
        for (String common : commonWords) {
            if (word.contains(common)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 寻找实体间的间接连接
     */
    private void findIndirectConnections(List<Entity> entities, List<String> evidences, List<ReasoningStep> reasoningPath) {
        if (entities.size() < 2) return;
        
        logger.debug("Searching for indirect connections between {} entities", entities.size());
        
        // 尝试找到任意两个实体之间的路径
        for (int i = 0; i < entities.size() - 1; i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                Entity entity1 = entities.get(i);
                Entity entity2 = entities.get(j);
                
                List<String> path = findShortestPath(entity1.getId(), entity2.getId(), 4);
                
                if (!path.isEmpty()) {
                    evidences.add(String.format("[Indirect Connection] %s 与 %s 通过 %d 跳连接", 
                                 entity1.getName(), entity2.getName(), path.size() - 1));
                    
                    // 构建路径描述
                    StringBuilder pathDesc = new StringBuilder();
                    for (int k = 0; k < path.size() - 1; k++) {
                        Entity fromEntity = graphDatabase.findEntity(path.get(k));
                        Entity toEntity = graphDatabase.findEntity(path.get(k + 1));
                        
                        if (fromEntity != null && toEntity != null) {
                            pathDesc.append(fromEntity.getName()).append(" -> ").append(toEntity.getName());
                            if (k < path.size() - 2) pathDesc.append(" -> ");
                        }
                    }
                    
                    evidences.add(String.format("[Connection Path] %s", pathDesc.toString()));
                    logger.debug("Found indirect connection: {}", pathDesc.toString());
                }
            }
        }
    }
    
    /**
     * 使用BFS查找最短路径
     */
    private List<String> findShortestPath(String sourceId, String targetId, int maxDepth) {
        if (sourceId.equals(targetId)) {
            return Arrays.asList(sourceId);
        }
        
        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(Arrays.asList(sourceId));
        visited.add(sourceId);
        
        while (!queue.isEmpty()) {
            List<String> currentPath = queue.poll();
            String currentId = currentPath.get(currentPath.size() - 1);
            
            if (currentPath.size() > maxDepth) {
                continue;
            }
            
            try {
                List<com.tog.graph.core.Relation> relations = graphDatabase.getEntityRelations(currentId);
                
                for (com.tog.graph.core.Relation relation : relations) {
                    String nextId = relation.getTargetEntityId().equals(currentId) ? 
                                   relation.getSourceEntityId() : relation.getTargetEntityId();
                    
                    if (nextId.equals(targetId)) {
                        List<String> foundPath = new ArrayList<>(currentPath);
                        foundPath.add(nextId);
                        return foundPath;
                    }
                    
                    if (!visited.contains(nextId)) {
                        visited.add(nextId);
                        List<String> newPath = new ArrayList<>(currentPath);
                        newPath.add(nextId);
                        queue.offer(newPath);
                    }
                }
            } catch (Exception e) {
                logger.debug("Error exploring relations for entity: {}", currentId, e);
            }
        }
        
        return new ArrayList<>(); // 未找到路径
    }
    
    /**
     * 基于Schema规划查询策略
     */
    private QueryPlan planSchemaAwareQuery(String question, EntityExtractionResult extraction, GraphSchema schema) {
        QueryPlan plan = new QueryPlan();
        
        // 为每个抽取的实体创建搜索步骤
        for (var entity : extraction.getExtractedEntities()) {
            QueryStep step = new QueryStep();
            step.setStepType(QueryStep.StepType.ENTITY_SEARCH);
            step.setTargetEntityType(entity.getType());
            step.setSearchText(entity.getText());
            step.setSearchProperties(entity.getSearchProperties());
            step.setConfidence(entity.getConfidence());
            
            plan.addStep(step);
        }
        
        // 为相关关系创建遍历步骤
        for (var relationship : extraction.getRelationships()) {
            QueryStep step = new QueryStep();
            step.setStepType(QueryStep.StepType.RELATIONSHIP_TRAVERSAL);
            step.setRelationshipType(relationship.getType());
            step.setConfidence(relationship.getConfidence());
            
            plan.addStep(step);
        }
        
        // 设置查询意图
        plan.setQueryIntent(extraction.getQueryIntent());
        
        return plan;
    }
    
    /**
     * 执行查询计划
     */
    private ReasoningResult executeQueryPlan(String question, QueryPlan plan, GraphSchema schema) {
        List<ReasoningStep> reasoningPath = new ArrayList<>();
        List<String> evidences = new ArrayList<>();
        
        // 执行实体搜索步骤
        Map<String, List<Entity>> foundEntities = new HashMap<>();
        List<Entity> allFoundEntities = new ArrayList<>();
        
        for (QueryStep step : plan.getSteps()) {
            if (step.getStepType() == QueryStep.StepType.ENTITY_SEARCH) {
                try {
                    logger.debug("Searching for entity: '{}' of type: '{}'", step.getSearchText(), step.getTargetEntityType());
                    
                    var searchResults = searchEngine.searchEntities(step.getSearchText(), 10);
                    
                    List<Entity> entities = searchResults.stream()
                            .map(scored -> scored.getEntity())
                            .collect(Collectors.toList());
                    
                    String entityKey = step.getTargetEntityType().equals("ANY") ? 
                                     step.getSearchText() : step.getTargetEntityType();
                    foundEntities.put(entityKey, entities);
                    allFoundEntities.addAll(entities);
                    
                    // 记录推理步骤
                    for (var scored : searchResults) {
                        evidences.add(String.format("[Depth 0, Score %.3f] %s", 
                                    scored.getScore(), scored.getEntity().getName()));
                        
                        logger.debug("Found entity: {} (type: {}, score: {:.3f})", 
                                   scored.getEntity().getName(), 
                                   scored.getEntity().getType(), 
                                   scored.getScore());
                    }
                    
                } catch (Exception e) {
                    logger.error("Failed to execute entity search step for: {}", step.getSearchText(), e);
                }
            }
        }
        
        // 探索找到的实体的邻居关系
        if (!allFoundEntities.isEmpty()) {
            logger.debug("Exploring relationships for {} found entities", allFoundEntities.size());
            exploreEntityRelationships(allFoundEntities, evidences, reasoningPath);
            
            // 如果是关系查询但没有找到直接连接，尝试寻找间接路径
            if (question.contains("关系") && allFoundEntities.size() >= 2) {
                findIndirectConnections(allFoundEntities, evidences, reasoningPath);
            }
        }
        
        // 生成基于Schema的答案
        String answer = generateSchemaAwareAnswer(question, plan, foundEntities, schema);
        
        return new ReasoningResult(question, answer, reasoningPath, evidences);
    }
    
    /**
     * 多跳探索实体的邻居关系
     */
    private void exploreEntityRelationships(List<Entity> entities, List<String> evidences, List<ReasoningStep> reasoningPath) {
        Set<String> visitedEntities = new HashSet<>();
        
        // 从配置中获取最大深度
        int maxDepth = config.getMaxDepth();
        int maxWidth = config.getWidth();
        
        logger.debug("Starting multi-hop exploration: maxDepth={}, maxWidth={}", maxDepth, maxWidth);
        
        // 初始化当前层的实体
        List<Entity> currentLevelEntities = entities.subList(0, Math.min(maxWidth, entities.size()));
        
        // 多跳探索
        for (int depth = 1; depth <= maxDepth; depth++) {
            List<Entity> nextLevelEntities = new ArrayList<>();
            
            logger.debug("Exploring depth {}: {} entities", depth, currentLevelEntities.size());
            
            for (Entity entity : currentLevelEntities) {
                if (visitedEntities.contains(entity.getId())) {
                    continue;
                }
                visitedEntities.add(entity.getId());
                
                try {
                    // 获取实体的所有关系
                    List<com.tog.graph.core.Relation> relations = graphDatabase.getEntityRelations(entity.getId());
                    
                    // 按关系类型分组并限制数量
                    Map<String, List<com.tog.graph.core.Relation>> relationsByType = relations.stream()
                            .collect(Collectors.groupingBy(com.tog.graph.core.Relation::getType));
                    
                    int relationCount = 0;
                    for (Map.Entry<String, List<com.tog.graph.core.Relation>> entry : relationsByType.entrySet()) {
                        if (relationCount >= maxWidth) break; // 限制总关系数量
                        
                        String relationType = entry.getKey();
                        List<com.tog.graph.core.Relation> relationsOfType = entry.getValue();
                        
                        // 限制每种关系类型的数量
                        int typeLimit = Math.min(2, maxWidth - relationCount);
                        List<com.tog.graph.core.Relation> limitedRelations = 
                                relationsOfType.subList(0, Math.min(typeLimit, relationsOfType.size()));
                        
                        for (com.tog.graph.core.Relation relation : limitedRelations) {
                            try {
                                // 获取目标实体
                                String targetEntityId = relation.getTargetEntityId().equals(entity.getId()) ? 
                                                       relation.getSourceEntityId() : relation.getTargetEntityId();
                                
                                Entity targetEntity = graphDatabase.findEntity(targetEntityId);
                                if (targetEntity != null && !visitedEntities.contains(targetEntityId)) {
                                    
                                    // 计算关系的相关性分数
                                    double relationScore = calculateRelationRelevance(relation, relationType, depth);
                                    
                                    // 只保留相关性较高的关系
                                    if (relationScore > config.getRelationThreshold()) {
                                        // 添加到证据中
                                        evidences.add(String.format("[Depth %d, Score %.3f] %s -[%s]-> %s", 
                                                     depth, relationScore, entity.getName(), relationType, targetEntity.getName()));
                                        
                                        // 添加到推理路径中
                                        ReasoningStep step = new ReasoningStep(entity, relation, targetEntity, relationScore);
                                        reasoningPath.add(step);
                                        
                                        // 添加到下一层探索
                                        if (depth < maxDepth && nextLevelEntities.size() < maxWidth) {
                                            nextLevelEntities.add(targetEntity);
                                        }
                                        
                                        logger.debug("Found relationship at depth {}: {} -[{}]-> {} (score: {:.3f})", 
                                                   depth, entity.getName(), relationType, targetEntity.getName(), relationScore);
                                    }
                                }
                                
                                relationCount++;
                                
                            } catch (Exception e) {
                                logger.debug("Failed to explore relation: {}", relation.getType(), e);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Failed to explore relationships for entity: {}", entity.getName(), e);
                }
            }
            
            // 准备下一层探索
            currentLevelEntities = nextLevelEntities;
            
            // 如果没有更多实体可探索，提前结束
            if (currentLevelEntities.isEmpty()) {
                logger.debug("No more entities to explore at depth {}, stopping", depth + 1);
                break;
            }
        }
        
        logger.debug("Multi-hop exploration completed: visited {} entities, found {} evidence pieces", 
                   visitedEntities.size(), evidences.size());
    }
    
    /**
     * 计算关系的相关性分数
     */
    private double calculateRelationRelevance(com.tog.graph.core.Relation relation, String relationType, int depth) {
        double baseScore = 0.8;
        
        // 深度惩罚：越深的关系分数越低
        double depthPenalty = Math.pow(0.8, depth - 1);
        
        // 关系类型权重
        double typeWeight = getRelationTypeWeight(relationType);
        
        // 最终分数
        double score = baseScore * depthPenalty * typeWeight;
        
        return Math.max(0.1, Math.min(1.0, score));
    }
    
    /**
     * 获取关系类型的权重 这里是硬编码，不好
     */
    private double getRelationTypeWeight(String relationType) {
        // 根据关系类型的重要性分配权重
        switch (relationType.toLowerCase()) {
            case "主治":
            case "治疗":
            case "治疗方法":
                return 1.0;
            case "组成":
            case "包含":
            case "含有":
                return 0.9;
            case "因果关系":
            case "影响":
                return 0.8;
            case "对应":
            case "相关":
                return 0.7;
            case "禁忌":
            case "副作用":
                return 0.6;
            default:
                return 0.5;
        }
    }
    
    /**
     * 生成基于Schema的答案
     */
    private String generateSchemaAwareAnswer(String question, QueryPlan plan, 
                                           Map<String, List<Entity>> foundEntities, GraphSchema schema) {
        
        StringBuilder context = new StringBuilder();
        
        // 检查是否找到了相关实体
        boolean foundRelevantEntities = false;
        int totalEntities = 0;
        
        for (List<Entity> entities : foundEntities.values()) {
            totalEntities += entities.size();
            if (!entities.isEmpty()) {
                foundRelevantEntities = true;
            }
        }
        
        if (foundRelevantEntities) {
            context.append("找到以下相关实体:\n\n");
            
            // 构建发现的实体上下文
            for (Map.Entry<String, List<Entity>> entry : foundEntities.entrySet()) {
                String entityKey = entry.getKey();
                List<Entity> entities = entry.getValue();
                
                if (!entities.isEmpty()) {
                    context.append(String.format("搜索 '%s' 找到 %d 个相关实体:\n", entityKey, entities.size()));
                    
                    for (Entity entity : entities.subList(0, Math.min(5, entities.size()))) {
                        context.append(String.format("- %s (类型: %s)\n", 
                                     entity.getName(), entity.getType()));
                        
                        // 添加实体的重要属性
                        if (entity.getProperties() != null) {
                            for (Map.Entry<String, Object> prop : entity.getProperties().entrySet()) {
                                String key = prop.getKey();
                                Object value = prop.getValue();
                                if (value != null && !key.equals("id") && !key.equals("name") && 
                                    value.toString().length() < 50) {
                                    context.append(String.format("  %s: %s\n", key, value));
                                }
                            }
                        }
                    }
                    context.append("\n");
                }
            }
        } else {
            context.append("未找到直接相关的实体。\n\n");
        }
        
        // 添加关系信息
        if (!plan.getSteps().isEmpty()) {
            context.append("相关关系:\n");
            for (QueryStep step : plan.getSteps()) {
                if (step.getStepType() == QueryStep.StepType.RELATIONSHIP_TRAVERSAL) {
                    context.append(String.format("- %s 关系\n", step.getRelationshipType()));
                }
            }
            context.append("\n");
        }
        
        // 使用PromptManager获取答案生成模板
        String prompt = promptManager.getPrompt("answer-generation", 
                PromptManager.params("question", question, 
                                   "query_intent", plan.getQueryIntent(), 
                                   "context", context.toString()));
        
        try {
            return llmService.generate(prompt, 0.2, 256);
        } catch (Exception e) {
            logger.error("Failed to generate schema-aware answer", e);
            return "基于Schema的推理生成答案时出错";
        }
    }
    
    /**
     * 实体抽取结果
     */
    public static class EntityExtractionResult {
        private final List<ExtractedEntity> extractedEntities = new ArrayList<>();
        private final List<ExtractedRelationship> relationships = new ArrayList<>();
        private String queryIntent = "";
        
        public void addEntity(String text, String type, double confidence, List<String> searchProperties) {
            extractedEntities.add(new ExtractedEntity(text, type, confidence, searchProperties));
        }
        
        public void addRelationship(String type, double confidence) {
            relationships.add(new ExtractedRelationship(type, confidence));
        }
        
        public List<ExtractedEntity> getExtractedEntities() { return extractedEntities; }
        public List<ExtractedRelationship> getRelationships() { return relationships; }
        public String getQueryIntent() { return queryIntent; }
        public void setQueryIntent(String queryIntent) { this.queryIntent = queryIntent; }
    }
    
    public static class ExtractedEntity {
        private final String text;
        private final String type;
        private final double confidence;
        private final List<String> searchProperties;
        
        public ExtractedEntity(String text, String type, double confidence, List<String> searchProperties) {
            this.text = text;
            this.type = type;
            this.confidence = confidence;
            this.searchProperties = searchProperties;
        }
        
        public String getText() { return text; }
        public String getType() { return type; }
        public double getConfidence() { return confidence; }
        public List<String> getSearchProperties() { return searchProperties; }
    }
    
    public static class ExtractedRelationship {
        private final String type;
        private final double confidence;
        
        public ExtractedRelationship(String type, double confidence) {
            this.type = type;
            this.confidence = confidence;
        }
        
        public String getType() { return type; }
        public double getConfidence() { return confidence; }
    }
}