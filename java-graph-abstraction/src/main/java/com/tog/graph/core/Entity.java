package com.tog.graph.core;

import java.util.Map;
import java.util.HashMap;

/**
 * 图实体类
 * 表示知识图谱中的实体节点
 */
public class Entity {
    private String id;
    private String name;
    private String type;
    private Map<String, Object> properties;
    
    public Entity() {
        this.properties = new HashMap<>();
    }
    
    public Entity(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }
    
    public Entity(String id, String name, String type) {
        this(id, name);
        this.type = type;
    }
    
    public Entity(String id, String name, String type, String description, Map<String, Object> properties) {
        this(id, name, type);
        this.properties = properties != null ? properties : new HashMap<>();
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
    
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return this.properties.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("Entity{id='%s', name='%s', type='%s'}", id, name, type);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Entity entity = (Entity) obj;
        return id != null ? id.equals(entity.id) : entity.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}