package com.tog.graph.api.dto;

/**
 * 实体搜索请求
 */
public class EntitySearchRequest {
    private String query;
    private Integer limit;
    
    public EntitySearchRequest() {}
    
    public EntitySearchRequest(String query, Integer limit) {
        this.query = query;
        this.limit = limit;
    }
    
    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}