package com.tog.graph.schema;

import java.util.*;

/**
 * 节点类型信息
 */
public class NodeTypeInfo {
    
    private final String label;
    private long count = 0;
    private final Map<String, PropertyInfo> properties = new HashMap<>();
    
    public NodeTypeInfo(String label) {
        this.label = label;
    }
    
    public String getLabel() { return label; }
    
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    
    public Collection<PropertyInfo> getProperties() { return properties.values(); }
    
    public PropertyInfo getProperty(String name) { return properties.get(name); }
    
    public void addProperty(PropertyInfo property) {
        properties.put(property.getName(), property);
    }
    
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }
    
    /**
     * 获取最常见的属性
     */
    public List<PropertyInfo> getMostCommonProperties(int limit) {
        return properties.values().stream()
                .sorted((a, b) -> Long.compare(b.getFrequency(), a.getFrequency()))
                .limit(limit)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * 获取可搜索的属性（通常是字符串类型且频率高的属性）
     */
    public List<PropertyInfo> getSearchableProperties() {
        return properties.values().stream()
                .filter(prop -> prop.getPrimaryType().equals("STRING"))
                .filter(prop -> prop.getFrequency() > count * 0.5) // 至少50%的节点有这个属性
                .sorted((a, b) -> Long.compare(b.getFrequency(), a.getFrequency()))
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    @Override
    public String toString() {
        return String.format("NodeType{label='%s', count=%d, properties=%d}", 
                           label, count, properties.size());
    }
}