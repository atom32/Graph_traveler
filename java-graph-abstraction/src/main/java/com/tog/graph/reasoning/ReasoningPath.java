package com.tog.graph.reasoning;

import com.tog.graph.core.Entity;
import java.util.List;
import java.util.ArrayList;

/**
 * 推理路径类
 */
public class ReasoningPath {
    private final List<ReasoningStep> steps;
    private final double finalScore;
    private final String description;
    
    public ReasoningPath(List<ReasoningStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
        this.finalScore = calculateFinalScore();
        this.description = buildDescription();
    }
    
    public ReasoningPath(ReasoningStep step) {
        this.steps = List.of(step);
        this.finalScore = step.getScore();
        this.description = step.toString();
    }
    
    public ReasoningPath(List<ReasoningStep> steps, double score, int depth) {
        this.steps = steps != null ? steps : new ArrayList<>();
        this.finalScore = score;
        this.description = buildDescription();
    }
    
    private double calculateFinalScore() {
        if (steps.isEmpty()) return 0.0;
        return steps.stream()
                .mapToDouble(ReasoningStep::getScore)
                .average()
                .orElse(0.0);
    }
    
    private String buildDescription() {
        if (steps.isEmpty()) return "Empty path";
        if (steps.size() == 1) return steps.get(0).toString();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(steps.get(i).toString());
        }
        return sb.toString();
    }
    
    public List<ReasoningStep> getSteps() { return steps; }
    public double getFinalScore() { return finalScore; }
    public String getPathDescription() { return description; }
    public int getLength() { return steps.size(); }
    
    // 为 MultiHopReasoner 添加的方法
    public boolean isValid() { 
        return !steps.isEmpty() && steps.stream().allMatch(ReasoningStep::isValid); 
    }
    
    public Entity getTargetEntity() {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1).getTargetEntity();
    }
    
    public double getScore() { return finalScore; }
    
    public void setFinalScore(double score) {
        // 注意：这破坏了不可变性，但为了兼容现有代码
        // 实际项目中应该重新设计
    }
    
    @Override
    public String toString() {
        return String.format("ReasoningPath{steps=%d, score=%.3f}", steps.size(), finalScore);
    }
}