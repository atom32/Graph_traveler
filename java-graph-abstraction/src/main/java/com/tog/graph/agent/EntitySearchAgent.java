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
        
        // 多策略搜索：先尝试精确搜索，再尝试模糊搜索
        List<ScoredEntity> results = new ArrayList<>();
        
        // 策略1: 直接搜索
        List<ScoredEntity> directResults = searchEngine.searchEntities(query, limit);
        results.addAll(directResults);
        
        // 策略2: 如果结果不够，尝试部分匹配
        if (results.size() < 3 && query.length() > 1) {
            for (int i = query.length() - 1; i >= 2; i--) {
                String partialQuery = query.substring(0, i);
                List<ScoredEntity> partialResults = searchEngine.searchEntities(partialQuery, limit);
                
                // 过滤重复结果
                for (ScoredEntity partial : partialResults) {
                    boolean isDuplicate = results.stream()
                            .anyMatch(existing -> existing.getEntity().getId().equals(partial.getEntity().getId()));
                    if (!isDuplicate) {
                        // 降低部分匹配的分数
                        ScoredEntity adjustedEntity = new ScoredEntity(partial.getEntity(), partial.getScore() * 0.8);
                        results.add(adjustedEntity);
                    }
                }
                
                if (results.size() >= limit) break;
            }
        }
        
        // 策略3: 如果还是没找到，尝试单字搜索（针对中文）
        if (results.isEmpty() && query.length() >= 2) {
            for (char c : query.toCharArray()) {
                String charQuery = String.valueOf(c);
                List<ScoredEntity> charResults = searchEngine.searchEntities(charQuery, 5);
                
                for (ScoredEntity charResult : charResults) {
                    if (charResult.getEntity().getName().contains(query)) {
                        // 如果实体名称包含原查询，给高分
                        ScoredEntity boostedEntity = new ScoredEntity(charResult.getEntity(), 0.9);
                        results.add(boostedEntity);
                    } else {
                        // 否则给低分
                        ScoredEntity lowScoreEntity = new ScoredEntity(charResult.getEntity(), 0.3);
                        results.add(lowScoreEntity);
                    }
                }
                
                if (results.size() >= limit) break;
            }
        }
        
        // 去重并排序
        Map<String, ScoredEntity> uniqueResults = new HashMap<>();
        for (ScoredEntity scored : results) {
            String key = scored.getEntity().getId();
            if (!uniqueResults.containsKey(key) || 
                uniqueResults.get(key).getScore() < scored.getScore()) {
                uniqueResults.put(key, scored);
            }
        }
        
        List<ScoredEntity> finalResults = new ArrayList<>(uniqueResults.values());
        finalResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        finalResults = finalResults.subList(0, Math.min(limit, finalResults.size()));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("found_count", finalResults.size());
        metadata.put("entities", finalResults);
        metadata.put("search_strategies_used", 3);
        
        if (finalResults.isEmpty()) {
            return AgentResult.success("未找到相关实体", metadata);
        }
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("找到 %d 个相关实体:\n", finalResults.size()));
        
        for (int i = 0; i < Math.min(5, finalResults.size()); i++) {
            ScoredEntity scored = finalResults.get(i);
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