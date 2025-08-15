package com.tog.graph.agent;

import com.tog.graph.core.GraphDatabase;
import com.tog.graph.core.Entity;
import com.tog.graph.core.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关系分析智能体
 * 专门负责分析实体间的关系和连接模式
 */
public class RelationshipAnalysisAgent implements Agent {
    
    private static final Logger logger = LoggerFactory.getLogger(RelationshipAnalysisAgent.class);
    
    private final GraphDatabase graphDatabase;
    private AgentStatus status = AgentStatus.INITIALIZING;
    
    public RelationshipAnalysisAgent(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }
    
    @Override
    public String getId() {
        return "relationship-analysis-agent";
    }
    
    @Override
    public String getName() {
        return "关系分析智能体";
    }
    
    @Override
    public String getDescription() {
        return "专门负责分析实体间的关系、发现连接模式和路径";
    }
    
    @Override
    public List<String> getSupportedTaskTypes() {
        return Arrays.asList("relationship_analysis", "path_finding", "connection_discovery", "relation_summary");
    }
    
    @Override
    public boolean canHandle(String taskType, String taskDescription) {
        return getSupportedTaskTypes().contains(taskType);
    }
    
    @Override
    public AgentResult execute(String taskType, String taskDescription, Map<String, Object> context) {
        if (status != AgentStatus.READY) {
            return AgentResult.failure("Agent not ready: " + status);
        }
        
        status = AgentStatus.BUSY;
        
        try {
            logger.info("RelationshipAnalysisAgent executing task: {} - {}", taskType, taskDescription);
            
            switch (taskType) {
                case "relationship_analysis":
                    return analyzeEntityRelationships(context);
                case "path_finding":
                    return findPaths(context);
                case "connection_discovery":
                    return discoverConnections(context);
                case "relation_summary":
                    return summarizeRelations(context);
                default:
                    return AgentResult.failure("Unsupported task type: " + taskType);
            }
            
        } catch (Exception e) {
            logger.error("RelationshipAnalysisAgent execution failed", e);
            return AgentResult.failure("Execution failed: " + e.getMessage());
        } finally {
            status = AgentStatus.READY;
        }
    }
    
    private AgentResult analyzeEntityRelationships(Map<String, Object> context) {
        String entityId = (String) context.get("entity_id");
        if (entityId == null) {
            return AgentResult.failure("Missing entity_id in context");
        }
        
        Entity entity = graphDatabase.findEntity(entityId);
        if (entity == null) {
            return AgentResult.failure("Entity not found: " + entityId);
        }
        
        List<Relation> relations = graphDatabase.getEntityRelations(entityId);
        
        // 按关系类型分组
        Map<String, List<Relation>> relationsByType = relations.stream()
                .collect(Collectors.groupingBy(Relation::getType));
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("实体 '%s' 的关系分析:\n", entity.getName()));
        result.append(String.format("总关系数: %d\n", relations.size()));
        result.append(String.format("关系类型数: %d\n\n", relationsByType.size()));
        
        for (Map.Entry<String, List<Relation>> entry : relationsByType.entrySet()) {
            String relationType = entry.getKey();
            List<Relation> relationsOfType = entry.getValue();
            
            result.append(String.format("关系类型: %s (%d个)\n", relationType, relationsOfType.size()));
            
            // 显示前3个关系
            for (int i = 0; i < Math.min(3, relationsOfType.size()); i++) {
                Relation relation = relationsOfType.get(i);
                String targetId = relation.getTargetEntityId().equals(entityId) ? 
                                relation.getSourceEntityId() : relation.getTargetEntityId();
                Entity targetEntity = graphDatabase.findEntity(targetId);
                
                if (targetEntity != null) {
                    result.append(String.format("  - %s\n", targetEntity.getName()));
                }
            }
            result.append("\n");
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("entity", entity);
        metadata.put("total_relations", relations.size());
        metadata.put("relation_types", relationsByType.keySet());
        metadata.put("relations_by_type", relationsByType);
        
        return AgentResult.success(result.toString(), metadata);
    }
    
    private AgentResult findPaths(Map<String, Object> context) {
        String sourceId = (String) context.get("source_id");
        String targetId = (String) context.get("target_id");
        int maxDepth = (Integer) context.getOrDefault("max_depth", 3);
        
        if (sourceId == null || targetId == null) {
            return AgentResult.failure("Missing source_id or target_id in context");
        }
        
        // 简化的路径查找实现
        List<List<String>> paths = findPathsBFS(sourceId, targetId, maxDepth);
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("从 %s 到 %s 的路径 (最大深度: %d):\n", sourceId, targetId, maxDepth));
        
        if (paths.isEmpty()) {
            result.append("未找到连接路径\n");
        } else {
            result.append(String.format("找到 %d 条路径:\n", paths.size()));
            for (int i = 0; i < Math.min(3, paths.size()); i++) {
                List<String> path = paths.get(i);
                result.append(String.format("路径 %d: %s\n", i + 1, String.join(" -> ", path)));
            }
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_id", sourceId);
        metadata.put("target_id", targetId);
        metadata.put("max_depth", maxDepth);
        metadata.put("paths", paths);
        
        return AgentResult.success(result.toString(), metadata);
    }
    
    private AgentResult discoverConnections(Map<String, Object> context) {
        String entityId = (String) context.get("entity_id");
        int depth = (Integer) context.getOrDefault("depth", 2);
        
        if (entityId == null) {
            return AgentResult.failure("Missing entity_id in context");
        }
        
        Set<String> visited = new HashSet<>();
        Map<String, Integer> entityDepths = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        
        queue.offer(entityId);
        visited.add(entityId);
        entityDepths.put(entityId, 0);
        
        while (!queue.isEmpty() && entityDepths.values().stream().anyMatch(d -> d < depth)) {
            String currentId = queue.poll();
            int currentDepth = entityDepths.get(currentId);
            
            if (currentDepth >= depth) continue;
            
            List<Relation> relations = graphDatabase.getEntityRelations(currentId);
            for (Relation relation : relations) {
                String nextId = relation.getTargetEntityId().equals(currentId) ? 
                              relation.getSourceEntityId() : relation.getTargetEntityId();
                
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    entityDepths.put(nextId, currentDepth + 1);
                    queue.offer(nextId);
                }
            }
        }
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("实体 %s 的连接发现 (深度: %d):\n", entityId, depth));
        result.append(String.format("发现 %d 个连接的实体:\n", visited.size() - 1));
        
        for (Map.Entry<String, Integer> entry : entityDepths.entrySet()) {
            if (!entry.getKey().equals(entityId)) {
                Entity entity = graphDatabase.findEntity(entry.getKey());
                if (entity != null) {
                    result.append(String.format("深度 %d: %s\n", entry.getValue(), entity.getName()));
                }
            }
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("entity_id", entityId);
        metadata.put("depth", depth);
        metadata.put("connected_entities", visited);
        metadata.put("entity_depths", entityDepths);
        
        return AgentResult.success(result.toString(), metadata);
    }
    
    private AgentResult summarizeRelations(Map<String, Object> context) {
        // 获取数据库中的关系类型统计
        List<String> relationTypes = graphDatabase.getAllRelationshipTypes();
        Map<String, Long> relationCounts = new HashMap<>();
        
        for (String relationType : relationTypes) {
            long count = graphDatabase.getRelationshipCount(relationType);
            relationCounts.put(relationType, count);
        }
        
        StringBuilder result = new StringBuilder();
        result.append("关系类型统计:\n");
        result.append(String.format("总关系类型数: %d\n\n", relationTypes.size()));
        
        relationCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    result.append(String.format("%s: %d 个关系\n", entry.getKey(), entry.getValue()));
                });
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("relation_types", relationTypes);
        metadata.put("relation_counts", relationCounts);
        
        return AgentResult.success(result.toString(), metadata);
    }
    
    private List<List<String>> findPathsBFS(String sourceId, String targetId, int maxDepth) {
        List<List<String>> paths = new ArrayList<>();
        Queue<List<String>> queue = new LinkedList<>();
        queue.offer(Arrays.asList(sourceId));
        
        while (!queue.isEmpty() && paths.size() < 5) { // 限制路径数量
            List<String> currentPath = queue.poll();
            String currentId = currentPath.get(currentPath.size() - 1);
            
            if (currentPath.size() > maxDepth) continue;
            
            if (currentId.equals(targetId) && currentPath.size() > 1) {
                paths.add(new ArrayList<>(currentPath));
                continue;
            }
            
            List<Relation> relations = graphDatabase.getEntityRelations(currentId);
            for (Relation relation : relations) {
                String nextId = relation.getTargetEntityId().equals(currentId) ? 
                              relation.getSourceEntityId() : relation.getTargetEntityId();
                
                if (!currentPath.contains(nextId)) {
                    List<String> newPath = new ArrayList<>(currentPath);
                    newPath.add(nextId);
                    queue.offer(newPath);
                }
            }
        }
        
        return paths;
    }
    
    @Override
    public AgentStatus getStatus() {
        return status;
    }
    
    @Override
    public void initialize() {
        logger.info("Initializing RelationshipAnalysisAgent...");
        status = AgentStatus.READY;
        logger.info("RelationshipAnalysisAgent initialized successfully");
    }
    
    @Override
    public void shutdown() {
        logger.info("Shutting down RelationshipAnalysisAgent...");
        status = AgentStatus.SHUTDOWN;
        logger.info("RelationshipAnalysisAgent shutdown completed");
    }
}