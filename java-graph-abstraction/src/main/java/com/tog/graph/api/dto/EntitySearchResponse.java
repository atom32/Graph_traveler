package com.tog.graph.api.dto;

import java.util.List;

/**
 * 实体搜索响应
 */
public class EntitySearchResponse {
    private boolean success;
    private List<EntityDto> entities;
    private int totalFound;
    private long executionTime;
    private String error;
    
    public EntitySearchResponse() {}
    
    public EntitySearchResponse(boolean success, List<EntityDto> entities, int totalFound, long executionTime) {
        this.success = success;
        this.entities = entities;
        this.totalFound = totalFound;
        this.executionTime = executionTime;
    }
    
    public EntitySearchResponse(boolean success, List<EntityDto> entities, int totalFound, long executionTime, String error) {
        this(success, entities, totalFound, executionTime);
        this.error = error;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public List<EntityDto> getEntities() { return entities; }
    public void setEntities(List<EntityDto> entities) { this.entities = entities; }
    
    public int getTotalFound() { return totalFound; }
    public void setTotalFound(int totalFound) { this.totalFound = totalFound; }
    
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}