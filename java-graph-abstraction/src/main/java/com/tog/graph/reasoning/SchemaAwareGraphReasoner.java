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
import com.tog.graph.schema.DatabaseNeutralSchemaAnalyzer;
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
    private final DatabaseNeutralSchemaAnalyzer schemaAnalyzer;
    private GraphSchema schema;
    
    public SchemaAwareGraphReasoner(GraphDatabase graphDatabase, SearchEngine searchEngine, 
                                   LLMService llmService, ReasoningConfig config) {
        this.graphDatabase = graphDatabase;
        this.searchEngine = searchEngine;
        this.llmService = llmService;
        this.config = config;
        this.fallbackReasoner = new GraphReasoner(graphDatabase, searchEngine, llmService, config);
        this.promptManager = PromptManager.getInstance();
        this.schemaAnalyzer = new DatabaseNeutralSchemaAnalyzer(graphDatabase);
    }
    
    /**
     * 基于Schema的智能推理
     */
    public ReasoningResult reason(String question) {
        try {
            logger.info("Starting schema-aware reasoning for: {}", question);
            
            // 1. 获取Schema信息
            this.schema = getGraphSchema();
            if (this.schema == null) {
                logger.warn("No schema available, falling back to standard reasoning");
                return fallbackReasoner.reason(question);
            }
            
            // 2. 使用LLM和Schema进行智能实体抽取
            EntityExtractionResult extraction = performSchemaGuidedEntityExtraction(question, this.schema);
            
            if (extraction.getExtractedEntities().isEmpty()) {
                logger.warn("No entities extracted, falling back to standard reasoning");
                return fallbackReasoner.reason(question);
            }
            
            // 3. 基于Schema规划查询策略
            QueryPlan queryPlan = planSchemaAwareQuery(question, extraction, this.schema);
            
            // 4. 执行查询计划
            ReasoningResult result = executeQueryPlan(question, queryPlan, this.schema);
            
            logger.info("Schema-aware reasoning completed successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("Schema-aware reasoning failed, falling back", e);
            return fallbackReasoner.reason(question);
        }
    }


    /**
     * 简化的推理得出结论（不进行复杂查询）
     */
    public ReasoningResult reasonWithContext(String question, String context) {
        try {
            String prompt = promptManager.getPrompt("answer-generation",
                PromptManager.params("question", question, "context", context));
            String answer = llmService.generate(prompt, 0.2, 256);
            return new ReasoningResult(question, answer, Collections.emptyList(), Collections.emptyList());
        } catch (Exception e) {
            logger.error("Failed to generate answer with context", e);
            return new ReasoningResult(question, "生成结论失败", Collections.emptyList(), Collections.emptyList());
        }
    }
    
    /**
     * 获取图Schema - 优先使用已缓存的schema，避免重复分析
     */
    private GraphSchema getGraphSchema() {
        try {
            // 优先从AdvancedGraphSearchEngine获取已缓存的schema
            if (searchEngine instanceof AdvancedGraphSearchEngine) {
                logger.debug("Getting cached schema from AdvancedGraphSearchEngine...");
                GraphSchema cachedSchema = ((AdvancedGraphSearchEngine) searchEngine).getSchema();
                if (cachedSchema != null) {
                    logger.debug("Using cached schema: {} node types, {} relationship types", 
                               cachedSchema.getNodeTypes().size(), 
                               cachedSchema.getRelationshipTypes().size());
                    return cachedSchema;
                }
            }
            
            // 如果没有缓存的schema，才进行分析
            logger.debug("No cached schema available, analyzing using DatabaseNeutralSchemaAnalyzer...");
            GraphSchema analyzedSchema = schemaAnalyzer.analyzeSchema();
            
            if (analyzedSchema != null) {
                logger.info("Schema analysis completed: {} node types, {} relationship types", 
                           analyzedSchema.getNodeTypes().size(), 
                           analyzedSchema.getRelationshipTypes().size());
                
                // 打印Schema摘要用于调试
                logger.debug("Schema Summary:\n{}", analyzedSchema.getSummary());
                
                return analyzedSchema;
            }
            
            logger.warn("No schema available from any source");
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to get graph schema", e);
            return null;
        }
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
            
            // 3. 从 LLM 响应中提取关系类型
            List<String> relationshipTypes = extractRelationshipTypesFromLLMResponse(response);
            for (String relType : relationshipTypes) {
                result.addRelationship(relType, 0.8);
                logger.debug("Added relationship type from LLM: {}", relType);
            }

            // 如果没有从 LLM 获取到关系类型，则基于问题和Schema推断
            if (relationshipTypes.isEmpty()) {
                List<String> inferredRelationships = inferRelationshipTypes(question, schema);
                for (String relType : inferredRelationships) {
                    result.addRelationship(relType, 0.7);
                    logger.debug("Added inferred relationship type: {}", relType);
                }
            }

            // 4. 从 LLM 响应中提取查询意图，而不是硬编码
            String extractedIntent = extractQueryIntentFromLLMResponse(response);
            if (extractedIntent != null && !extractedIntent.isEmpty()) {
                result.setQueryIntent(extractedIntent);
                logger.debug("Extracted query intent from LLM: {}", extractedIntent);
            } else {
                // 基于问题和Schema动态推断查询意图
                String inferredIntent = inferQueryIntent(question);
                result.setQueryIntent(inferredIntent);
                logger.debug("Inferred query intent: {}", inferredIntent);
            }
            
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
     * 基于Schema判断是否为常见词汇
     */
    private boolean isCommonWord(String word) {
        if (schema == null) {
            return isBasicCommonWord(word);
        }
        
        // 从Schema中获取停用词列表
        List<String> stopWords = schema.getStopWords();
        if (stopWords != null) {
            return stopWords.stream().anyMatch(word::contains);
        }
        
        return isBasicCommonWord(word);
    }
    
    /**
     * 基础常见词汇判断（最小集合）
     */
    private boolean isBasicCommonWord(String word) {
        // 只保留最基础的停用词，避免硬编码
        return word.length() <= 1 || 
               word.matches(".*[的是在了和与为中].*") ||
               word.matches(".*[什么怎么哪里为什么].*");
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
     * 基于Schema动态获取关系类型的权重
     */
    private double getRelationTypeWeight(String relationType) {
        if (schema == null) {
            return 0.5; // 默认权重
        }
        
        // 从Schema中获取关系类型的重要性配置
        Map<String, Double> relationWeights = schema.getRelationTypeWeights();
        if (relationWeights != null && relationWeights.containsKey(relationType)) {
            return relationWeights.get(relationType);
        }
        
        // 基于关系类型的语义特征动态计算权重
        return calculateSemanticWeight(relationType);
    }
    
    /**
     * 基于语义特征计算关系权重
     */
    private double calculateSemanticWeight(String relationType) {
        String lowerType = relationType.toLowerCase();
        
        // 高权重关键词
        if (containsAny(lowerType, schema.getHighPriorityRelationKeywords())) {
            return 1.0;
        }
        
        // 中权重关键词
        if (containsAny(lowerType, schema.getMediumPriorityRelationKeywords())) {
            return 0.7;
        }
        
        // 低权重关键词
        if (containsAny(lowerType, schema.getLowPriorityRelationKeywords())) {
            return 0.4;
        }
        
        return 0.5; // 默认权重
    }
    
    /**
     * 检查字符串是否包含任何关键词
     */
    private boolean containsAny(String text, List<String> keywords) {
        if (keywords == null) return false;
        return keywords.stream().anyMatch(text::contains);
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
    
    /**
     * 从LLM响应中提取实体
     */
    private List<String> extractEntitiesFromLLMResponse(String response) {
        List<String> entities = new ArrayList<>();
        
        // 尝试JSON解析
        if (response.contains("\"entities\"")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("\"text\"")) {
                    String entity = extractQuotedValue(line, "text");
                    if (entity != null && !entity.isEmpty()) {
                        entities.add(entity);
                    }
                }
            }
        }
        
        return entities;
    }
    
    /**
     * 从问题中提取实体（基于Schema）
     */
    private List<String> extractEntitiesFromQuestion(String question) {
        List<String> entities = new ArrayList<>();
        
        // 1. 使用Schema中定义的实体识别模式
        if (schema != null) {
            entities.addAll(extractEntitiesBySchemaPatterns(question));
        }
        
        // 2. 通用实体识别
        entities.addAll(extractGenericEntities(question));
        
        return entities.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 基于Schema模式提取实体
     */
    private List<String> extractEntitiesBySchemaPatterns(String question) {
        List<String> entities = new ArrayList<>();
        
        // 获取Schema中定义的实体识别规则
        Map<String, List<String>> entityPatterns = schema.getEntityExtractionPatterns();
        if (entityPatterns != null) {
            for (Map.Entry<String, List<String>> entry : entityPatterns.entrySet()) {
                String entityType = entry.getKey();
                List<String> patterns = entry.getValue();
                
                for (String pattern : patterns) {
                    entities.addAll(extractByPattern(question, pattern));
                }
            }
        }
        
        return entities;
    }
    
    /**
     * 通用实体提取
     */
    private List<String> extractGenericEntities(String question) {
        List<String> entities = new ArrayList<>();
        
        // 分词并过滤
        String[] words = question.split("[\\s，。！？、的和与]+");
        for (String word : words) {
            if (isValidEntity(word)) {
                entities.add(word);
            }
        }
        
        return entities;
    }
    
    /**
     * 基于模式提取实体
     */
    private List<String> extractByPattern(String text, String pattern) {
        List<String> entities = new ArrayList<>();
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            while (m.find()) {
                entities.add(m.group(1));
            }
        } catch (Exception e) {
            logger.debug("Failed to apply pattern: {}", pattern, e);
        }
        return entities;
    }
    
    /**
     * 推断实体类型（基于Schema）
     */
    private String inferEntityType(String entity, GraphSchema schema) {
        if (schema == null) {
            return "ANY";
        }
        
        // 使用Schema中定义的类型推断规则
        Map<String, List<String>> typeInferenceRules = schema.getEntityTypeInferenceRules();
        if (typeInferenceRules != null) {
            for (Map.Entry<String, List<String>> entry : typeInferenceRules.entrySet()) {
                String entityType = entry.getKey();
                List<String> rules = entry.getValue();
                
                for (String rule : rules) {
                    if (matchesRule(entity, rule)) {
                        return entityType;
                    }
                }
            }
        }
        
        // 检查Schema中是否有匹配的节点类型
        for (String nodeType : schema.getNodeTypeNames()) {
            if (nodeType.contains(entity) || entity.contains(nodeType)) {
                return nodeType;
            }
        }
        
        return "ANY";
    }
    
    /**
     * 检查实体是否匹配规则
     */
    private boolean matchesRule(String entity, String rule) {
        try {
            return entity.matches(rule);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 计算实体置信度（基于Schema）
     */
    private double calculateEntityConfidence(String entity, String question) {
        double confidence = 0.5; // 基础置信度
        
        if (schema != null) {
            // 使用Schema中定义的置信度计算规则
            Map<String, Double> confidenceRules = schema.getEntityConfidenceRules();
            if (confidenceRules != null) {
                for (Map.Entry<String, Double> entry : confidenceRules.entrySet()) {
                    String rule = entry.getKey();
                    Double boost = entry.getValue();
                    
                    if (matchesRule(entity, rule)) {
                        confidence += boost;
                    }
                }
            }
        }
        
        // 基础规则
        if (entity.length() >= 2 && entity.length() <= 4) {
            confidence += 0.1;
        }
        
        if (question.indexOf(entity) < question.length() / 2) {
            confidence += 0.1;
        }
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * 获取最优搜索属性（基于Schema）
     */
    private List<String> getOptimalSearchProperties(String entity, String entityType, GraphSchema schema) {
        List<String> properties = new ArrayList<>();
        
        // 基础属性
        properties.add("name");
        
        if (schema != null) {
            // 从Schema中获取实体类型的推荐搜索属性
            Map<String, List<String>> typeProperties = schema.getEntityTypeSearchProperties();
            if (typeProperties != null && typeProperties.containsKey(entityType)) {
                properties.addAll(typeProperties.get(entityType));
            }
        }
        
        return properties;
    }
    
    /**
     * 推断查询意图（基于Schema）
     */
    private String inferQueryIntent(String question) {
        if (schema != null) {
            Map<String, List<String>> intentPatterns = schema.getQueryIntentPatterns();
            if (intentPatterns != null) {
                for (Map.Entry<String, List<String>> entry : intentPatterns.entrySet()) {
                    String intent = entry.getKey();
                    List<String> patterns = entry.getValue();
                    
                    for (String pattern : patterns) {
                        if (question.contains(pattern)) {
                            return intent;
                        }
                    }
                }
            }
        }
        
        return "通用知识查询";
    }
    
    /**
     * 判断是否为有效实体
     */
    private boolean isValidEntity(String word) {
        return word.length() >= 2 && 
               !isCommonWord(word) && 
               !word.matches(".*[？！。，、].*") &&
               word.matches(".*[\\u4e00-\\u9fa5].*"); // 包含中文
    }
    
    /**
     * 从字符串中提取引号内的值
     */
    private String extractQuotedValue(String line, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(line);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }
    
    /**
     * 从LLM响应中提取关系类型
     */
    private List<String> extractRelationshipTypesFromLLMResponse(String response) {
        List<String> relationshipTypes = new ArrayList<>();
        
        // 尝试从JSON响应中提取关系类型
        if (response.contains("\"relationships\"") || response.contains("\"relations\"")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("\"type\"") || line.contains("\"relation\"")) {
                    String relType = extractQuotedValue(line, "type");
                    if (relType == null) {
                        relType = extractQuotedValue(line, "relation");
                    }
                    if (relType != null && !relType.isEmpty()) {
                        relationshipTypes.add(relType);
                    }
                }
            }
        }
        
        return relationshipTypes;
    }
    
    /**
     * 基于问题和Schema推断关系类型
     */
    private List<String> inferRelationshipTypes(String question, GraphSchema schema) {
        List<String> inferredTypes = new ArrayList<>();
        
        if (schema == null) {
            return inferredTypes;
        }
        
        // 基于问题中的关键词推断相关关系类型
        String lowerQuestion = question.toLowerCase();
        
        // 从Schema中获取所有关系类型，并计算相关性
        for (RelationshipTypeInfo relType : schema.getRelationshipTypes()) {
            String relationshipType = relType.getType();
            double relevance = calculateRelationshipRelevanceForQuestion(lowerQuestion, relationshipType);
            
            if (relevance > 0.3) { // 相关性阈值
                inferredTypes.add(relationshipType);
                logger.debug("Inferred relationship type '{}' with relevance {:.3f}", relationshipType, relevance);
            }
        }
        
        // 如果没有找到相关关系，使用默认的高频关系类型
        if (inferredTypes.isEmpty()) {
            List<RelationshipTypeInfo> commonTypes = schema.getMostCommonRelationshipTypes(3);
            for (RelationshipTypeInfo relType : commonTypes) {
                inferredTypes.add(relType.getType());
            }
        }
        
        return inferredTypes;
    }
    
    /**
     * 计算关系类型与问题的相关性
     */
    private double calculateRelationshipRelevanceForQuestion(String question, String relationshipType) {
        double relevance = 0.0;
        String lowerRelType = relationshipType.toLowerCase();
        
        // 直接匹配
        if (question.contains(lowerRelType)) {
            relevance += 0.8;
        }
        
        // 语义相关性检查
        if (question.contains("关系") && (lowerRelType.contains("相关") || lowerRelType.contains("连接"))) {
            relevance += 0.6;
        }
        
        if (question.contains("治疗") && lowerRelType.contains("治")) {
            relevance += 0.7;
        }
        
        if (question.contains("组成") && (lowerRelType.contains("包含") || lowerRelType.contains("组成"))) {
            relevance += 0.7;
        }
        
        return Math.min(1.0, relevance);
    }
    
    /**
     * 从LLM响应中提取查询意图
     */
    private String extractQueryIntentFromLLMResponse(String response) {
        // 尝试从JSON响应中提取意图
        if (response.contains("\"intent\"") || response.contains("\"purpose\"")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                String intent = extractQuotedValue(line, "intent");
                if (intent == null) {
                    intent = extractQuotedValue(line, "purpose");
                }
                if (intent != null && !intent.isEmpty()) {
                    return intent;
                }
            }
        }
        
        return null;
    }
}