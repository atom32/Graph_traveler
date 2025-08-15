package com.tog.graph.agent;

import java.util.Map;
import java.util.HashMap;

/**
 * 智能体执行结果
 */
public class AgentResult {
    
    private final boolean success;
    private final String result;
    private final String error;
    private final Map<String, Object> metadata;
    private final long executionTime;
    
    public AgentResult(boolean success, String result, String error, Map<String, Object> metadata, long executionTime) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.executionTime = executionTime;
    }
    
    public static AgentResult success(String result) {
        return new AgentResult(true, result, null, new HashMap<>(), 0);
    }
    
    public static AgentResult success(String result, Map<String, Object> metadata) {
        return new AgentResult(true, result, null, metadata, 0);
    }
    
    public static AgentResult failure(String error) {
        return new AgentResult(false, null, error, new HashMap<>(), 0);
    }
    
    public static AgentResult failure(String error, Map<String, Object> metadata) {
        return new AgentResult(false, null, error, metadata, 0);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getResult() { return result; }
    public String getError() { return error; }
    public Map<String, Object> getMetadata() { return metadata; }
    public long getExecutionTime() { return executionTime; }
}