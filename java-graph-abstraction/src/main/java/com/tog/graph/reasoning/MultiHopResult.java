package com.tog.graph.reasoning;

import java.util.List;
import java.util.ArrayList;

/**
 * 多跳推理结果类
 */
public class MultiHopResult {
    private final String question;
    private final String answer;
    private final List<ReasoningStep> reasoningPath;
    private final List<String> evidences;
    private final int hopCount;
    private final double confidence;
    
    public MultiHopResult(String question, String answer, List<ReasoningStep> reasoningPath, 
                         List<String> evidences, int hopCount, double confidence) {
        this.question = question;
        this.answer = answer;
        this.reasoningPath = reasoningPath != null ? reasoningPath : new ArrayList<>();
        this.evidences = evidences != null ? evidences : new ArrayList<>();
        this.hopCount = hopCount;
        this.confidence = confidence;
    }
    
    // 从 ReasoningResult 转换的构造函数
    public MultiHopResult(ReasoningResult result) {
        this.question = result.getQuestion();
        this.answer = result.getAnswer();
        this.reasoningPath = result.getReasoningPath();
        this.evidences = result.getEvidences();
        this.hopCount = result.getReasoningPath().size();
        this.confidence = 0.8; // 默认置信度
    }
    
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public List<ReasoningStep> getReasoningPath() { return reasoningPath; }
    public List<String> getEvidences() { return evidences; }
    public int getHopCount() { return hopCount; }
    public double getConfidence() { return confidence; }
    
    // 为演示程序添加的方法
    public String getBestAnswer() { return answer; }
    public double getOverallConfidence() { return confidence; }
    public List<ReasoningPath> getPaths() { 
        return reasoningPath.stream()
                .map(ReasoningPath::new)
                .collect(java.util.stream.Collectors.toList()); 
    }
    public String getQualityRating() { return confidence > 0.8 ? "High" : confidence > 0.5 ? "Medium" : "Low"; }
    public List<ReasoningPath> getTopPaths(int limit) { 
        return reasoningPath.stream()
                .limit(limit)
                .map(ReasoningPath::new)
                .collect(java.util.stream.Collectors.toList()); 
    }
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("hopCount", hopCount);
        stats.put("confidence", confidence);
        stats.put("pathLength", reasoningPath.size());
        return stats;
    }
    public String getSummary() {
        return String.format("Question: %s\nAnswer: %s\nHops: %d\nConfidence: %.3f", 
                           question, answer, hopCount, confidence);
    }
    
    @Override
    public String toString() {
        return String.format("MultiHopResult{question='%s', answer='%s', hops=%d, confidence=%.3f}", 
                           question, answer, hopCount, confidence);
    }
}