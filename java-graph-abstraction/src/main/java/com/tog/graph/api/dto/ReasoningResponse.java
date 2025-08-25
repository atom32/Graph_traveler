package com.tog.graph.api.dto;

import java.util.List;

/**
 * 推理查询响应
 */
public class ReasoningResponse {
    private boolean success;
    private String answer;
    private double confidence;
    private List<String> reasoningSteps;
    private List<String> sourceEntities;
    private long executionTime;
    private String reasoningType;
    private String error;
    
    public ReasoningResponse() {}
    
    public ReasoningResponse(boolean success, String answer, double confidence, 
                           List<String> reasoningSteps, List<String> sourceEntities, 
                           long executionTime, String reasoningType) {
        this.success = success;
        this.answer = answer;
        this.confidence = confidence;
        this.reasoningSteps = reasoningSteps;
        this.sourceEntities = sourceEntities;
        this.executionTime = executionTime;
        this.reasoningType = reasoningType;
    }
    
    public ReasoningResponse(boolean success, String answer, double confidence, 
                           List<String> reasoningSteps, List<String> sourceEntities, 
                           long executionTime, String reasoningType, String error) {
        this(success, answer, confidence, reasoningSteps, sourceEntities, executionTime, reasoningType);
        this.error = error;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public List<String> getReasoningSteps() { return reasoningSteps; }
    public void setReasoningSteps(List<String> reasoningSteps) { this.reasoningSteps = reasoningSteps; }
    
    public List<String> getSourceEntities() { return sourceEntities; }
    public void setSourceEntities(List<String> sourceEntities) { this.sourceEntities = sourceEntities; }
    
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    
    public String getReasoningType() { return reasoningType; }
    public void setReasoningType(String reasoningType) { this.reasoningType = reasoningType; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}