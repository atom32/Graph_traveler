package com.tog.graph.reasoning;

import com.tog.graph.core.Entity;
import com.tog.graph.core.Relation;

/**
 * 增强的推理步骤
 * 包含更多上下文信息和元数据
 */
public class ReasoningStep {
    private final Entity sourceEntity;
    private final Relation relation;
    private final Entity targetEntity;
    private final double score;
    
    // 元数据
    private int depth;
    private long timestamp;
    private String reasoning;
    private double confidence;
    
    public ReasoningStep(Entity sourceEntity, Relation relation, Entity targetEntity, double score) {
        this.sourceEntity = sourceEntity;
        this.relation = relation;
        this.targetEntity = targetEntity;
        this.score = score;
        this.depth = 0;
        this.timestamp = System.currentTimeMillis();
        this.confidence = score;
    }
    
    public ReasoningStep(Entity sourceEntity, Relation relation, Entity targetEntity, 
                        double score, int depth, String reasoning) {
        this(sourceEntity, relation, targetEntity, score);
        this.depth = depth;
        this.reasoning = reasoning;
    }
    
    // Getters
    public Entity getSourceEntity() { return sourceEntity; }
    public Relation getRelation() { return relation; }
    public Entity getTargetEntity() { return targetEntity; }
    public double getScore() { return score; }
    public int getDepth() { return depth; }
    public long getTimestamp() { return timestamp; }
    public String getReasoning() { return reasoning; }
    public double getConfidence() { return confidence; }
    
    // Setters
    public void setDepth(int depth) { this.depth = depth; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    // 为 AsyncReasoningExecutor 添加的方法
    public String getStepId() {
        return sourceEntity.getId() + "-" + relation.getType() + "-" + targetEntity.getId();
    }
    
    public String getDescription() {
        return toString();
    }
    
    public String getType() {
        return relation.getType();
    }
    
    public java.util.List<String> getDependencies() {
        return new java.util.ArrayList<>(); // 简化实现，返回空依赖列表
    }
    
    /**
     * 获取步骤的详细描述
     */
    public String getDetailedDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("Depth %d: %s -[%s]-> %s", 
                   depth, sourceEntity.getName(), relation.getType(), targetEntity.getName()));
        desc.append(String.format(" (score: %.3f, confidence: %.3f)", score, confidence));
        
        if (reasoning != null && !reasoning.isEmpty()) {
            desc.append(" - ").append(reasoning);
        }
        
        return desc.toString();
    }
    
    /**
     * 检查步骤是否有效
     */
    public boolean isValid() {
        return sourceEntity != null && 
               relation != null && 
               targetEntity != null && 
               score >= 0.0;
    }
    
    /**
     * 计算步骤的重要性分数
     */
    public double getImportanceScore() {
        double depthPenalty = 1.0 / (depth + 1); // 深度越深重要性越低
        double scoreFactor = Math.max(0.1, score); // 确保最小分数
        return scoreFactor * depthPenalty * confidence;
    }
    
    @Override
    public String toString() {
        return String.format("Depth %d: %s -[%s]-> %s (score: %.3f)", 
                           depth, sourceEntity.getName(), relation.getType(), 
                           targetEntity.getName(), score);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ReasoningStep that = (ReasoningStep) obj;
        return sourceEntity.getId().equals(that.sourceEntity.getId()) &&
               relation.getType().equals(that.relation.getType()) &&
               targetEntity.getId().equals(that.targetEntity.getId());
    }
    
    @Override
    public int hashCode() {
        return (sourceEntity.getId() + relation.getType() + targetEntity.getId()).hashCode();
    }
}