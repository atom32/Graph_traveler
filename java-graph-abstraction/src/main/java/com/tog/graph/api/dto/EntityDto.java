package com.tog.graph.api.dto;

import java.util.Map;

/**
 * 实体数据传输对象
 */
public class EntityDto {
    private String id;
    private String name;
    private String type;
    private Map<String, Object> properties;
    private double score;
    
    public EntityDto() {}
    
    public EntityDto(String id, String name, String type, Map<String, Object> properties, double score) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.properties = properties;
        this.score = score;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}