package com.tog.graph.reasoning;

import java.util.*;

/**
 * 基于Schema的查询计划
 */
public class QueryPlan {
    
    private final List<QueryStep> steps = new ArrayList<>();
    private String queryIntent = "";
    private double overallConfidence = 0.0;
    
    public void addStep(QueryStep step) {
        steps.add(step);
        updateOverallConfidence();
    }
    
    public List<QueryStep> getSteps() { return steps; }
    
    public String getQueryIntent() { return queryIntent; }
    public void setQueryIntent(String queryIntent) { this.queryIntent = queryIntent; }
    
    public double getOverallConfidence() { return overallConfidence; }
    
    private void updateOverallConfidence() {
        if (steps.isEmpty()) {
            overallConfidence = 0.0;
            return;
        }
        
        overallConfidence = steps.stream()
                .mapToDouble(QueryStep::getConfidence)
                .average()
                .orElse(0.0);
    }
    
    /**
     * 检查计划是否有效
     */
    public boolean isValid() {
        return !steps.isEmpty() && overallConfidence > 0.3;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryPlan{\n");
        sb.append("  intent: ").append(queryIntent).append("\n");
        sb.append("  confidence: ").append(String.format("%.3f", overallConfidence)).append("\n");
        sb.append("  steps: [\n");
        for (int i = 0; i < steps.size(); i++) {
            sb.append("    ").append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}