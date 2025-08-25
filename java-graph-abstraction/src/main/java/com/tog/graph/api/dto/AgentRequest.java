package com.tog.graph.api.dto;

import java.util.Map;

/**
 * 智能体协作查询请求
 */
public class AgentRequest {
    private String query;
    private Map<String, Object> context;
    
    public AgentRequest() {}
    
    public AgentRequest(String query) {
        this.query = query;
    }
    
    public AgentRequest(String query, Map<String, Object> context) {
        this.query = query;
        this.context = context;
    }
    
    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}