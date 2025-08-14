package com.tog.graph.search;

import com.tog.graph.core.Entity;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.core.Relation;
import com.tog.graph.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 增强的搜索引擎实现
 * 基于嵌入向量的语义搜索
 */
public class SimpleSearchEngine implements SearchEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleSearchEngine.class);
    
    private final GraphDatabase graphDatabase;
    private final EmbeddingService embeddingService;
    
    // 实体嵌入缓存
    private final Map<String, float[]> entityEmbeddingCache = new HashMap<>();
    private final Map<String, float[]> relationEmbeddingCache = new HashMap<>();
    
    public SimpleSearchEngine(GraphDatabase graphDatabase, EmbeddingService embeddingService) {
        this.graphDatabase = graphDatabase;
        this.embeddingService = embeddingService;
    }
    
    @Override
    public void initialize() {
        logger.info("SimpleSearchEngine initialized");
    }
    
    @Override
    public List<ScoredEntity> searchEntities(String query, int topK) {
        logger.debug("Searching entities for query: {}", query);
        
        List<Entity> allEntities = getAllEntities();
        if (allEntities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取查询的嵌入向量
        float[] queryEmbedding = embeddingService.getEmbedding(query);
        
        // 计算与所有实体的相似度
        List<ScoredEntity> scoredEntities = new ArrayList<>();
        
        for (Entity entity : allEntities) {
            try {
                float[] entityEmbedding = getEntityEmbedding(entity);
                double similarity = embeddingService.cosineSimilarity(queryEmbedding, entityEmbedding);
                
                if (similarity > 0.1) { // 过滤低相似度结果
                    scoredEntities.add(new ScoredEntity(entity, similarity));
                }
            } catch (Exception e) {
                logger.warn("Failed to calculate similarity for entity: {}", entity.getId(), e);
                // 使用文本相似度作为fallback
                double textSim = calculateTextSimilarity(query, entity.getName());
                if (textSim > 0.1) {
                    scoredEntities.add(new ScoredEntity(entity, textSim));
                }
            }
        }
        
        return scoredEntities.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ScoredRelation> scoreRelations(String query, List<Relation> relations) {
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.debug("Scoring {} relations for query: {}", relations.size(), query);
        
        // 获取查询的嵌入向量
        float[] queryEmbedding = embeddingService.getEmbedding(query);
        
        List<ScoredRelation> scoredRelations = new ArrayList<>();
        
        for (Relation relation : relations) {
            try {
                float[] relationEmbedding = getRelationEmbedding(relation);
                double similarity = embeddingService.cosineSimilarity(queryEmbedding, relationEmbedding);
                
                if (similarity > 0.05) { // 关系的阈值可以更低
                    scoredRelations.add(new ScoredRelation(relation, similarity));
                }
            } catch (Exception e) {
                logger.warn("Failed to calculate similarity for relation: {}", relation.getType(), e);
                // 使用文本相似度作为fallback
                double textSim = calculateTextSimilarity(query, relation.getType());
                if (textSim > 0.05) {
                    scoredRelations.add(new ScoredRelation(relation, textSim));
                }
            }
        }
        
        return scoredRelations.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());
    }
    
    @Override
    public double calculateSimilarity(String text1, String text2) {
        try {
            float[] embedding1 = embeddingService.getEmbedding(text1);
            float[] embedding2 = embeddingService.getEmbedding(text2);
            return embeddingService.cosineSimilarity(embedding1, embedding2);
        } catch (Exception e) {
            logger.warn("Failed to calculate embedding similarity, falling back to text similarity", e);
            return calculateTextSimilarity(text1, text2);
        }
    }
    
    @Override
    public double[] calculateSimilarities(String query, List<String> texts) {
        try {
            float[] queryEmbedding = embeddingService.getEmbedding(query);
            List<float[]> textEmbeddings = embeddingService.getEmbeddings(texts);
            return embeddingService.calculateSimilarities(queryEmbedding, textEmbeddings);
        } catch (Exception e) {
            logger.warn("Failed to calculate embedding similarities, falling back to text similarity", e);
            return texts.stream()
                    .mapToDouble(text -> calculateTextSimilarity(query, text))
                    .toArray();
        }
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
     * 获取实体的嵌入向量（带缓存）
     */
    private float[] getEntityEmbedding(Entity entity) {
        String cacheKey = entity.getId();
        
        if (entityEmbeddingCache.containsKey(cacheKey)) {
            return entityEmbeddingCache.get(cacheKey);
        }
        
        // 构建实体的文本表示
        String entityText = buildEntityText(entity);
        float[] embedding = embeddingService.getEmbedding(entityText);
        
        entityEmbeddingCache.put(cacheKey, embedding);
        return embedding;
    }
    
    /**
     * 获取关系的嵌入向量（带缓存）
     */
    private float[] getRelationEmbedding(Relation relation) {
        String cacheKey = relation.getType() + "_" + relation.getSourceEntityId() + "_" + relation.getTargetEntityId();
        
        if (relationEmbeddingCache.containsKey(cacheKey)) {
            return relationEmbeddingCache.get(cacheKey);
        }
        
        // 构建关系的文本表示
        String relationText = buildRelationText(relation);
        float[] embedding = embeddingService.getEmbedding(relationText);
        
        relationEmbeddingCache.put(cacheKey, embedding);
        return embedding;
    }
    
    /**
     * 构建实体的文本表示
     */
    private String buildEntityText(Entity entity) {
        StringBuilder text = new StringBuilder();
        
        // 添加实体名称
        if (entity.getName() != null && !entity.getName().isEmpty()) {
            text.append(entity.getName()).append(" ");
        }
        
        // 添加实体类型
        if (entity.getType() != null && !entity.getType().isEmpty()) {
            text.append(entity.getType()).append(" ");
        }
        
        // 添加实体属性
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
     * 构建关系的文本表示
     */
    private String buildRelationText(Relation relation) {
        StringBuilder text = new StringBuilder();
        
        // 添加关系类型
        text.append(relation.getType()).append(" ");
        
        // 尝试获取源实体和目标实体的名称
        try {
            Entity sourceEntity = graphDatabase.findEntity(relation.getSourceEntityId());
            Entity targetEntity = graphDatabase.findEntity(relation.getTargetEntityId());
            
            if (sourceEntity != null && sourceEntity.getName() != null) {
                text.append(sourceEntity.getName()).append(" ");
            }
            
            if (targetEntity != null && targetEntity.getName() != null) {
                text.append(targetEntity.getName()).append(" ");
            }
        } catch (Exception e) {
            logger.debug("Failed to get entity names for relation text", e);
        }
        
        // 添加关系属性
        if (relation.getProperties() != null) {
            relation.getProperties().forEach((key, value) -> {
                if (value != null && !key.equals("score")) {
                    text.append(key).append(" ").append(value.toString()).append(" ");
                }
            });
        }
        
        return text.toString().trim();
    }
    
    /**
     * 简单的文本相似度计算（基于Jaccard相似度）
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * 获取所有实体（简化实现，实际应用中需要分页或索引）
     */
    private List<Entity> getAllEntities() {
        // 这里应该从数据库中获取实体，为了demo简化处理
        try {
            List<Map<String, Object>> results = graphDatabase.executeQuery(
                "MATCH (n) RETURN n LIMIT 1000", new HashMap<>());
            
            // 简化实现：直接使用 Neo4j 数据库的 searchEntities 方法
            return graphDatabase.searchEntities("", 1000);
        } catch (Exception e) {
            logger.warn("Failed to get all entities, returning empty list", e);
            return new ArrayList<>();
        }
    }
}