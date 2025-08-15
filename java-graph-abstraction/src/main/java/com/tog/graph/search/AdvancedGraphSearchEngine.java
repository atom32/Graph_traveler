package com.tog.graph.search;

import com.tog.graph.core.Entity;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.core.Relation;
import com.tog.graph.embedding.EmbeddingService;
import com.tog.graph.schema.GraphSchemaAnalyzer;
import com.tog.graph.schema.GraphSchema;
import com.tog.graph.schema.SearchStrategy;
import com.tog.graph.schema.NodeTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于Schema分析的高级图搜索引擎
 * 在搜索之前先分析数据库结构，制定智能搜索策略
 */
public class AdvancedGraphSearchEngine implements SearchEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedGraphSearchEngine.class);
    
    private final GraphDatabase graphDatabase;
    private final EmbeddingService embeddingService;
    private final GraphSchemaAnalyzer schemaAnalyzer;
    private final SimpleSearchEngine fallbackEngine;
    
    // 缓存
    private GraphSchema cachedSchema;
    private final Map<String, SearchStrategy> strategyCache = new HashMap<>();
    
    public AdvancedGraphSearchEngine(GraphDatabase graphDatabase, EmbeddingService embeddingService) {
        this.graphDatabase = graphDatabase;
        this.embeddingService = embeddingService;
        this.schemaAnalyzer = new GraphSchemaAnalyzer(graphDatabase);
        this.fallbackEngine = new SimpleSearchEngine(graphDatabase, embeddingService);
    }
    
    @Override
    public void initialize() {
        logger.info("Initializing AdvancedGraphSearchEngine...");
        
        // 预先分析Schema
        try {
            cachedSchema = schemaAnalyzer.analyzeSchema();
            logger.info("Schema analysis completed: {}", cachedSchema.getSummary());
        } catch (Exception e) {
            logger.warn("Failed to analyze schema during initialization, will use fallback", e);
        }
        
        // 初始化fallback引擎
        fallbackEngine.initialize();
        
        logger.info("AdvancedGraphSearchEngine initialized successfully");
    }
    
    @Override
    public List<ScoredEntity> searchEntities(String query, int topK) {
        logger.debug("Advanced entity search for query: '{}', topK: {}", query, topK);
        
        try {
            // 1. 确保Schema已分析
            if (cachedSchema == null) {
                try {
                    cachedSchema = schemaAnalyzer.analyzeSchema();
                    logger.debug("Schema analysis completed for advanced search");
                } catch (Exception e) {
                    logger.warn("Schema analysis failed, using fallback search", e);
                    return fallbackEngine.searchEntities(query, topK);
                }
            }
            
            // 2. 基于Schema推荐搜索策略
            SearchStrategy strategy = getOrCreateSearchStrategy(query);
            
            if (!strategy.isEffective()) {
                logger.debug("Schema-based strategy not effective, using fallback search");
                return fallbackEngine.searchEntities(query, topK);
            }
            
            // 3. 执行基于Schema的智能搜索
            List<ScoredEntity> results = executeSchemaBasedSearch(query, strategy, topK);
            
            // 4. 如果结果不够，补充使用fallback搜索
            if (results.size() < topK / 2) {
                logger.debug("Schema-based search returned {} results, supplementing with fallback", results.size());
                List<ScoredEntity> fallbackResults = fallbackEngine.searchEntities(query, topK - results.size());
                results = mergeSearchResults(results, fallbackResults, topK);
            }
            
            logger.debug("Advanced search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Advanced search failed, falling back to simple search", e);
            return fallbackEngine.searchEntities(query, topK);
        }
    }
    
    /**
     * 获取或创建搜索策略
     */
    private SearchStrategy getOrCreateSearchStrategy(String query) {
        // 简单的缓存key（实际应该更复杂）
        String cacheKey = query.toLowerCase().trim();
        
        if (strategyCache.containsKey(cacheKey)) {
            return strategyCache.get(cacheKey);
        }
        
        SearchStrategy strategy = schemaAnalyzer.recommendSearchStrategy(query, cachedSchema);
        strategyCache.put(cacheKey, strategy);
        
        logger.debug("Created search strategy for '{}': {}", query, strategy);
        return strategy;
    }
    
    /**
     * 执行基于Schema的搜索
     */
    private List<ScoredEntity> executeSchemaBasedSearch(String query, SearchStrategy strategy, int topK) {
        List<ScoredEntity> allResults = new ArrayList<>();
        
        // 1. 针对每个相关的节点类型进行搜索
        for (String nodeType : strategy.getTopNodeTypes(3)) {
            List<ScoredEntity> typeResults = searchByNodeType(query, nodeType, strategy, topK / 2);
            allResults.addAll(typeResults);
        }
        
        // 2. 如果有生成的Cypher查询，执行它们
        List<String> cypherQueries = strategy.generateCypherQueries(query);
        for (String cypherQuery : cypherQueries.subList(0, Math.min(2, cypherQueries.size()))) {
            try {
                List<ScoredEntity> cypherResults = executeCypherSearch(cypherQuery, query);
                allResults.addAll(cypherResults);
            } catch (Exception e) {
                logger.debug("Cypher query failed: {}", cypherQuery, e);
            }
        }
        
        // 3. 去重并排序
        return deduplicateAndRank(allResults, topK);
    }
    
    /**
     * 按节点类型搜索
     */
    private List<ScoredEntity> searchByNodeType(String query, String nodeType, SearchStrategy strategy, int limit) {
        List<String> searchProperties = strategy.getBestSearchProperties(nodeType, 3);
        
        if (searchProperties.isEmpty()) {
            // 使用默认属性
            searchProperties = Arrays.asList("name", "title", "description");
        }
        
        List<ScoredEntity> results = new ArrayList<>();
        
        for (String property : searchProperties) {
            try {
                // 使用数据库的现有方法搜索实体
                List<Entity> entities = graphDatabase.searchEntitiesByProperty(property, query, limit);
                
                for (Entity entity : entities) {
                    // 检查实体类型是否匹配
                    if (nodeType.equals(entity.getType()) || 
                        (entity.getProperties() != null && entity.getProperties().containsValue(nodeType))) {
                        double score = calculatePropertyMatchScore(query, entity, property);
                        results.add(new ScoredEntity(entity, score));
                    }
                }
                
            } catch (Exception e) {
                logger.debug("Failed to search {}:{}", nodeType, property, e);
            }
        }
        
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 执行Cypher搜索
     */
    private List<ScoredEntity> executeCypherSearch(String cypherQuery, String originalQuery) {
        List<ScoredEntity> results = new ArrayList<>();
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("query", originalQuery);
            
            List<Map<String, Object>> queryResults = graphDatabase.executeQuery(cypherQuery, params);
            
            for (Map<String, Object> result : queryResults) {
                Entity entity = extractEntityFromResult(result);
                if (entity != null) {
                    double score = calculateSemanticScore(originalQuery, entity);
                    results.add(new ScoredEntity(entity, score));
                }
            }
            
        } catch (Exception e) {
            logger.debug("Cypher search failed: {}", cypherQuery, e);
        }
        
        return results;
    }
    
    /**
     * 从查询结果中提取实体
     */
    private Entity extractEntityFromResult(Map<String, Object> result) {
        try {
            // 查找结果中的节点对象
            Object nodeObj = result.get("n");
            if (nodeObj == null) {
                // 尝试其他可能的键名
                for (String key : result.keySet()) {
                    Object value = result.get(key);
                    if (value != null && value.toString().contains("Node")) {
                        nodeObj = value;
                        break;
                    }
                }
            }
            
            if (nodeObj != null) {
                // 由于我们无法直接访问Neo4j的Node类型，我们通过反射来获取属性
                try {
                    // 尝试获取节点的ID
                    String nodeId = null;
                    String nodeName = null;
                    String nodeType = null;
                    Map<String, Object> properties = new HashMap<>();
                    
                    // 使用反射获取节点信息
                    Class<?> nodeClass = nodeObj.getClass();
                    
                    // 获取节点ID
                    try {
                        Object idObj = nodeClass.getMethod("id").invoke(nodeObj);
                        nodeId = String.valueOf(idObj);
                    } catch (Exception e) {
                        logger.debug("Failed to get node id", e);
                    }
                    
                    // 获取节点属性
                    try {
                        Object propsObj = nodeClass.getMethod("asMap").invoke(nodeObj);
                        if (propsObj instanceof Map) {
                            properties = (Map<String, Object>) propsObj;
                            nodeName = (String) properties.get("name");
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to get node properties", e);
                    }
                    
                    // 获取节点标签
                    try {
                        Object labelsObj = nodeClass.getMethod("labels").invoke(nodeObj);
                        if (labelsObj instanceof Iterable) {
                            Iterator<?> labelIter = ((Iterable<?>) labelsObj).iterator();
                            if (labelIter.hasNext()) {
                                nodeType = labelIter.next().toString();
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to get node labels", e);
                    }
                    
                    // 创建Entity对象
                    if (nodeId != null) {
                        Entity entity = new Entity(nodeId, nodeName != null ? nodeName : nodeId);
                        entity.setType(nodeType);
                        entity.setProperties(properties);
                        return entity;
                    }
                    
                } catch (Exception e) {
                    logger.debug("Failed to extract node information via reflection", e);
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Failed to extract entity from result", e);
            return null;
        }
    }
    
    /**
     * 计算属性匹配分数
     */
    private double calculatePropertyMatchScore(String query, Entity entity, String property) {
        try {
            Object propertyValue = entity.getProperties().get(property);
            if (propertyValue == null) return 0.0;
            
            String valueStr = propertyValue.toString().toLowerCase();
            String queryLower = query.toLowerCase();
            
            // 简单的字符串匹配评分
            if (valueStr.equals(queryLower)) return 1.0;
            if (valueStr.contains(queryLower)) return 0.8;
            if (queryLower.contains(valueStr)) return 0.6;
            
            // 使用嵌入相似度
            float[] queryEmbedding = embeddingService.getEmbedding(query);
            float[] valueEmbedding = embeddingService.getEmbedding(valueStr);
            return embeddingService.cosineSimilarity(queryEmbedding, valueEmbedding);
            
        } catch (Exception e) {
            logger.debug("Failed to calculate property match score", e);
            return 0.0;
        }
    }
    
    /**
     * 计算语义分数
     */
    private double calculateSemanticScore(String query, Entity entity) {
        try {
            String entityText = buildEntityText(entity);
            float[] queryEmbedding = embeddingService.getEmbedding(query);
            float[] entityEmbedding = embeddingService.getEmbedding(entityText);
            return embeddingService.cosineSimilarity(queryEmbedding, entityEmbedding);
        } catch (Exception e) {
            logger.debug("Failed to calculate semantic score", e);
            return 0.5; // 默认分数
        }
    }
    
    /**
     * 构建实体的文本表示
     */
    private String buildEntityText(Entity entity) {
        StringBuilder text = new StringBuilder();
        
        if (entity.getName() != null) {
            text.append(entity.getName()).append(" ");
        }
        
        if (entity.getType() != null) {
            text.append(entity.getType()).append(" ");
        }
        
        // 添加重要属性
        if (entity.getProperties() != null) {
            entity.getProperties().forEach((key, value) -> {
                if (value != null && !key.equals("id") && !key.equals("name") && !key.equals("type")) {
                    text.append(key).append(" ").append(value.toString()).append(" ");
                }
            });
        }
        
        return text.toString().trim();
    }
    
    /**
     * 合并搜索结果
     */
    private List<ScoredEntity> mergeSearchResults(List<ScoredEntity> primary, List<ScoredEntity> secondary, int topK) {
        Map<String, ScoredEntity> merged = new HashMap<>();
        
        // 添加主要结果
        for (ScoredEntity entity : primary) {
            merged.put(entity.getEntity().getId(), entity);
        }
        
        // 添加次要结果（如果不重复）
        for (ScoredEntity entity : secondary) {
            String id = entity.getEntity().getId();
            if (!merged.containsKey(id)) {
                // 降低次要结果的分数
                ScoredEntity adjustedEntity = new ScoredEntity(entity.getEntity(), entity.getScore() * 0.8);
                merged.put(id, adjustedEntity);
            }
        }
        
        return merged.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }
    
    /**
     * 去重并排序
     */
    private List<ScoredEntity> deduplicateAndRank(List<ScoredEntity> results, int topK) {
        Map<String, ScoredEntity> deduped = new HashMap<>();
        
        for (ScoredEntity entity : results) {
            String id = entity.getEntity().getId();
            if (!deduped.containsKey(id) || deduped.get(id).getScore() < entity.getScore()) {
                deduped.put(id, entity);
            }
        }
        
        return deduped.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ScoredRelation> scoreRelations(String query, List<Relation> relations) {
        // 委托给fallback引擎
        return fallbackEngine.scoreRelations(query, relations);
    }
    
    @Override
    public double calculateSimilarity(String text1, String text2) {
        try {
            float[] embedding1 = embeddingService.getEmbedding(text1);
            float[] embedding2 = embeddingService.getEmbedding(text2);
            return embeddingService.cosineSimilarity(embedding1, embedding2);
        } catch (Exception e) {
            logger.debug("Failed to calculate similarity", e);
            return 0.0;
        }
    }
    
    @Override
    public double[] calculateSimilarities(String query, List<String> texts) {
        return fallbackEngine.calculateSimilarities(query, texts);
    }
    
    @Override
    public float[] getEmbedding(String text) {
        return embeddingService.getEmbedding(text);
    }
    
    @Override
    public List<float[]> getEmbeddings(List<String> texts) {
        return embeddingService.getEmbeddings(texts);
    }
    
    /**
     * 获取Schema信息（用于调试）
     */
    public GraphSchema getSchema() {
        if (cachedSchema == null) {
            cachedSchema = schemaAnalyzer.analyzeSchema();
        }
        return cachedSchema;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        cachedSchema = null;
        strategyCache.clear();
        schemaAnalyzer.clearCache();
    }
}