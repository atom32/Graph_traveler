package com.tog.graph.agent;

import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.ScoredEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 实体搜索智能体
 * 专门负责在知识图谱中搜索和识别实体
 */
public class EntitySearchAgent implements Agent {
    
    private static final Logger logger = LoggerFactory.getLogger(EntitySearchAgent.class);
    
    private final SearchEngine searchEngine;
    private AgentStatus status = AgentStatus.INITIALIZING;
    
    public EntitySearchAgent(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }
    
    @Override
    public String getId() {
        return "entity-search-agent";
    }
    
    @Override
    public String getName() {
        return "实体搜索智能体";
    }
    
    @Override
    public String getDescription() {
        return "专门负责在知识图谱中搜索和识别实体，支持语义搜索和精确匹配";
    }
    
    @Override
    public List<String> getSupportedTaskTypes() {
        return Arrays.asList("entity_search", "entity_identification", "semantic_search");
    }
    
    @Override
    public boolean canHandle(String taskType, String taskDescription) {
        return getSupportedTaskTypes().contains(taskType) && 
               taskDescription != null && !taskDescription.trim().isEmpty();
    }
    
    @Override
    public AgentResult execute(String taskType, String taskDescription, Map<String, Object> context) {
        if (status != AgentStatus.READY) {
            return AgentResult.failure("Agent not ready: " + status);
        }
        
        status = AgentStatus.BUSY;
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("EntitySearchAgent executing task: {} - {}", taskType, taskDescription);
            
            switch (taskType) {
                case "entity_search":
                    return performEntitySearch(taskDescription, context);
                case "entity_identification":
                    return performEntityIdentification(taskDescription, context);
                case "semantic_search":
                    return performSemanticSearch(taskDescription, context);
                default:
                    return AgentResult.failure("Unsupported task type: " + taskType);
            }
            
        } catch (Exception e) {
            logger.error("EntitySearchAgent execution failed", e);
            return AgentResult.failure("Execution failed: " + e.getMessage());
        } finally {
            status = AgentStatus.READY;
        }
    }
    
    private AgentResult performEntitySearch(String query, Map<String, Object> context) {
        int limit = (Integer) context.getOrDefault("limit", 10);
        
        List<ScoredEntity> results = searchEngine.searchEntities(query, limit);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("found_count", results.size());
        metadata.put("entities", results);
        
        if (results.isEmpty()) {
            return AgentResult.success("未找到相关实体", metadata);
        }
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("找到 %d 个相关实体:\n", results.size()));
        
        for (int i = 0; i < Math.min(5, results.size()); i++) {
            ScoredEntity scored = results.get(i);
            result.append(String.format("%d. %s (相似度: %.3f)\n", 
                         i + 1, scored.getEntity().getName(), scored.getScore()));
        }
        
        return AgentResult.success(result.toString(), metadata);
    }
    
    private AgentResult performEntityIdentification(String text, Map<String, Object> context) {
        // 从文本中识别实体
        String[] keywords = text.split("[\\s，。！？、]+");
        List<ScoredEntity> allResults = new ArrayList<>();
        
        for (String keyword : keywords) {
            if (keyword.length() > 1) {
                List<ScoredEntity> results = searchEngine.searchEntities(keyword, 3);
                allResults.addAll(results);
            }
        }
        
        // 去重并排序
        Map<String, ScoredEntity> uniqueResults = new HashMap<>();
        for (ScoredEntity scored : allResults) {
            String key = scored.getEntity().getId();
            if (!uniqueResults.containsKey(key) || 
                uniqueResults.get(key).getScore() < scored.getScore()) {
                uniqueResults.put(key, scored);
            }
        }
        
        List<ScoredEntity> finalResults = new ArrayList<>(uniqueResults.values());
        finalResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("input_text", text);
        metadata.put("identified_entities", finalResults);
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("从文本中识别出 %d 个实体:\n", finalResults.size()));
        
        for (ScoredEntity scored : finalResults.subList(0, Math.min(3, finalResults.size()))) {
            result.append(String.format("- %s (置信度: %.3f)\n", 
                         scored.getEntity().getName(), scored.getScore()));
        }
        
        return AgentResult.success(result.toString(), metadata);
    }
    
    private AgentResult performSemanticSearch(String query, Map<String, Object> context) {
        double threshold = (Double) context.getOrDefault("threshold", 0.3);
        int limit = (Integer) context.getOrDefault("limit", 10);
        
        List<ScoredEntity> results = searchEngine.searchEntities(query, limit * 2);
        
        // 过滤低相似度结果
        List<ScoredEntity> filteredResults = results.stream()
                .filter(scored -> scored.getScore() >= threshold)
                .limit(limit)
                .toList();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("threshold", threshold);
        metadata.put("semantic_results", filteredResults);
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("语义搜索找到 %d 个相关实体:\n", filteredResults.size()));
        
        for (ScoredEntity scored : filteredResults) {
            result.append(String.format("- %s (语义相似度: %.3f)\n", 
                         scored.getEntity().getName(), scored.getScore()));
        }
        
        return AgentResult.success(result.toString(), metadata);
    }
    
    @Override
    public AgentStatus getStatus() {
        return status;
    }
    
    @Override
    public void initialize() {
        logger.info("Initializing EntitySearchAgent...");
        searchEngine.initialize();
        status = AgentStatus.READY;
        logger.info("EntitySearchAgent initialized successfully");
    }
    
    @Override
    public void shutdown() {
        logger.info("Shutting down EntitySearchAgent...");
        status = AgentStatus.SHUTDOWN;
        logger.info("EntitySearchAgent shutdown completed");
    }
}