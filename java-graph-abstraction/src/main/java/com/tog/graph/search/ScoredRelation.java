package com.tog.graph.search;

import com.tog.graph.core.Relation;

/**
 * 带评分的关系
 */
public class ScoredRelation implements Comparable<ScoredRelation> {
    private final Relation relation;
    private final double score;
    
    public ScoredRelation(Relation relation, double score) {
        this.relation = relation;
        this.score = score;
    }
    
    public Relation getRelation() {
        return relation;
    }
    
    public double getScore() {
        return score;
    }
    
    @Override
    public int compareTo(ScoredRelation other) {
        return Double.compare(other.score, this.score); // 降序排列
    }
    
    @Override
    public String toString() {
        return String.format("ScoredRelation{relation=%s, score=%.4f}", relation, score);
    }
}