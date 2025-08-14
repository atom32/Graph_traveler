package com.tog.graph.reasoning;

import com.tog.graph.core.Entity;
import com.tog.graph.core.Relation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强的推理上下文
 * 支持多层推理、证据权重和推理路径追踪
 */
public class ReasoningContext {
    private final String question;
    private final long startTime;
    
    // 访问追踪
    private final Set<String> visitedEntityIds;
    private final Map<String, Integer> entityVisitCount;
    
    // 推理路径
    private final List<ReasoningStep> reasoningPath;
    private final Map<Integer, List<Entity>> entitiesByDepth;
    
    // 证据收集
    private final List<Evidence> evidences;
    private final Map<String, Double> evidenceScores;
    
    // 当前状态
    private List<Entity> currentEntities;
    private int currentDepth;
    private double totalConfidence;
    
    // 推理统计
    private int totalEntitiesExplored;
    private int totalRelationsExplored;
    
    public ReasoningContext(String question) {
        this.question = question;
        this.startTime = System.currentTimeMillis();
        this.visitedEntityIds = ConcurrentHashMap.newKeySet();
        this.entityVisitCount = new ConcurrentHashMap<>();
        this.reasoningPath = Collections.synchronizedList(new ArrayList<>());
        this.entitiesByDepth = new ConcurrentHashMap<>();
        this.evidences = Collections.synchronizedList(new ArrayList<>());
        this.evidenceScores = new ConcurrentHashMap<>();
        this.currentEntities = new ArrayList<>();
        this.currentDepth = 0;
        this.totalConfidence = 0.0;
        this.totalEntitiesExplored = 0;
        this.totalRelationsExplored = 0;
    }
    
    /**
     * 添加实体到当前层
     */
    public synchronized void addEntities(List<Entity> entities) {
        List<Entity> newEntities = new ArrayList<>();
        
        for (Entity entity : entities) {
            if (!visitedEntityIds.contains(entity.getId())) {
                newEntities.add(entity);
                visitedEntityIds.add(entity.getId());
                entityVisitCount.put(entity.getId(), 1);
                totalEntitiesExplored++;
            } else {
                // 增加访问计数
                entityVisitCount.merge(entity.getId(), 1, Integer::sum);
            }
        }
        
        currentEntities.addAll(newEntities);
        
        // 记录当前深度的实体
        entitiesByDepth.computeIfAbsent(currentDepth, k -> new ArrayList<>()).addAll(newEntities);
    }
    
    /**
     * 添加推理步骤
     */
    public synchronized void addReasoningStep(Entity source, Relation relation, Entity target, double score) {
        ReasoningStep step = new ReasoningStep(source, relation, target, score);
        step.setDepth(currentDepth);
        step.setTimestamp(System.currentTimeMillis());
        
        reasoningPath.add(step);
        visitedEntityIds.add(target.getId());
        totalRelationsExplored++;
        
        // 创建证据
        Evidence evidence = new Evidence(
            String.format("%s -[%s]-> %s", source.getName(), relation.getType(), target.getName()),
            score,
            currentDepth,
            System.currentTimeMillis() - startTime
        );
        
        evidences.add(evidence);
        evidenceScores.put(evidence.getDescription(), score);
        
        // 更新总置信度
        updateTotalConfidence(score);
    }
    
    /**
     * 检查实体是否已访问
     */
    public boolean hasVisited(Entity entity) {
        return visitedEntityIds.contains(entity.getId());
    }
    
    /**
     * 获取实体访问次数
     */
    public int getVisitCount(Entity entity) {
        return entityVisitCount.getOrDefault(entity.getId(), 0);
    }
    
    /**
     * 检查是否有足够的证据
     */
    public boolean hasEnoughEvidence() {
        return evidences.size() >= 5 || 
               totalConfidence > 2.0 || 
               currentDepth >= 3;
    }
    
    /**
     * 检查是否应该停止推理
     */
    public boolean shouldStopReasoning(int maxDepth, int maxEntities) {
        return currentDepth >= maxDepth || 
               totalEntitiesExplored >= maxEntities ||
               hasEnoughEvidence() ||
               (System.currentTimeMillis() - startTime) > 30000; // 30秒超时
    }
    
    /**
     * 进入下一个推理深度
     */
    public synchronized void nextDepth() {
        currentDepth++;
        currentEntities = new ArrayList<>();
    }
    
    /**
     * 获取最佳证据（按分数排序）
     */
    public List<Evidence> getBestEvidences(int limit) {
        return evidences.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .toList();
    }
    
    /**
     * 获取推理路径摘要
     */
    public String getReasoningPathSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Reasoning Summary for: %s\n", question));
        summary.append(String.format("Total time: %dms\n", System.currentTimeMillis() - startTime));
        summary.append(String.format("Entities explored: %d\n", totalEntitiesExplored));
        summary.append(String.format("Relations explored: %d\n", totalRelationsExplored));
        summary.append(String.format("Max depth reached: %d\n", currentDepth));
        summary.append(String.format("Total confidence: %.3f\n", totalConfidence));
        summary.append(String.format("Evidence count: %d\n", evidences.size()));
        
        return summary.toString();
    }
    
    /**
     * 更新总置信度
     */
    private void updateTotalConfidence(double score) {
        // 使用加权平均来更新置信度
        double weight = 1.0 / (currentDepth + 1); // 深度越深权重越小
        totalConfidence += score * weight;
    }
    
    // Getters
    public String getQuestion() { return question; }
    public long getStartTime() { return startTime; }
    public List<Entity> getCurrentEntities() { return currentEntities; }
    public void setCurrentEntities(List<Entity> currentEntities) { this.currentEntities = currentEntities; }
    public List<ReasoningStep> getReasoningPath() { return reasoningPath; }
    public List<Evidence> getEvidences() { return evidences; }
    public List<String> getEvidenceDescriptions() { 
        return evidences.stream().map(Evidence::getDescription).toList(); 
    }
    public int getCurrentDepth() { return currentDepth; }
    public double getTotalConfidence() { return totalConfidence; }
    public int getTotalEntitiesExplored() { return totalEntitiesExplored; }
    public int getTotalRelationsExplored() { return totalRelationsExplored; }
    public Map<Integer, List<Entity>> getEntitiesByDepth() { return entitiesByDepth; }
    
    /**
     * 证据类
     */
    public static class Evidence {
        private final String description;
        private final double score;
        private final int depth;
        private final long timestamp;
        
        public Evidence(String description, double score, int depth, long timestamp) {
            this.description = description;
            this.score = score;
            this.depth = depth;
            this.timestamp = timestamp;
        }
        
        public String getDescription() { return description; }
        public double getScore() { return score; }
        public int getDepth() { return depth; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("[Depth %d, Score %.3f] %s", depth, score, description);
        }
    }
}