package com.tog.graph.core;

import java.util.Map;
import java.util.HashMap;

/**
 * 图关系类
 * 表示知识图谱中实体间的关系
 */
public class Relation {
    private String id;
    private String type;
    private String sourceEntityId;
    private String targetEntityId;
    private double score;
    private boolean isHead; // 是否为头实体关系
    private Map<String, Object> properties;
    
    public Relation() {
        this.properties = new HashMap<>();
        this.score = 1.0;
    }
    
    public Relation(String type, String sourceEntityId, String targetEntityId) {
        this();
        this.type = type;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }
    
    public Relation(String id, String type, String sourceEntityId, String targetEntityId, Map<String, Object> properties) {
        this(type, sourceEntityId, targetEntityId);
        this.id = id;
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    public Relation(String type, String sourceEntityId, String targetEntityId, double score) {
        this(type, sourceEntityId, targetEntityId);
        this.score = score;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSourceEntityId() { return sourceEntityId; }
    public void setSourceEntityId(String sourceEntityId) { this.sourceEntityId = sourceEntityId; }
    
    public String getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(String targetEntityId) { this.targetEntityId = targetEntityId; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    
    public boolean isHead() { return isHead; }
    public void setHead(boolean head) { isHead = head; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return this.properties.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("Relation{type='%s', source='%s', target='%s', score=%.2f}", 
                           type, sourceEntityId, targetEntityId, score);
    }
}