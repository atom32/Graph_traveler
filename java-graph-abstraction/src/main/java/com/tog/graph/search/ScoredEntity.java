package com.tog.graph.search;

import com.tog.graph.core.Entity;

/**
 * 带评分的实体
 */
public class ScoredEntity implements Comparable<ScoredEntity> {
    private final Entity entity;
    private final double score;
    
    public ScoredEntity(Entity entity, double score) {
        this.entity = entity;
        this.score = score;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public double getScore() {
        return score;
    }
    
    @Override
    public int compareTo(ScoredEntity other) {
        return Double.compare(other.score, this.score); // 降序排列
    }
    
    @Override
    public String toString() {
        return String.format("ScoredEntity{entity=%s, score=%.4f}", entity, score);
    }
}