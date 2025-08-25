package com.tog.graph.api.dto;

import java.util.Map;

/**
 * 智能体响应
 */
public class AgentResponse {
    private boolean success;
    private String result;
    private Map<String, Object> metadata;
    private long executionTime;
    private String error;
    
    public AgentResponse() {}
    
    public AgentResponse(boolean success, String result, Map<String, Object> metadata, long executionTime, String error) {
        this.success = success;
        this.result = result;
        this.metadata = metadata;
        this.executionTime = executionTime;
        this.error = error;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}