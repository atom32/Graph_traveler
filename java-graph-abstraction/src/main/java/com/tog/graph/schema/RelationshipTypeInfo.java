package com.tog.graph.schema;

import java.util.*;

/**
 * 关系类型信息
 */
public class RelationshipTypeInfo {
    
    private final String type;
    private final Map<String, PropertyInfo> properties = new HashMap<>();
    private final List<RelationshipPattern> patterns = new ArrayList<>();
    
    public RelationshipTypeInfo(String type) {
        this.type = type;
    }
    
    public String getType() { return type; }
    
    public Collection<PropertyInfo> getProperties() { return properties.values(); }
    
    public PropertyInfo getProperty(String name) { return properties.get(name); }
    
    public void addProperty(PropertyInfo property) {
        properties.put(property.getName(), property);
    }
    
    public List<RelationshipPattern> getPatterns() { return patterns; }
    
    public void addPattern(String sourceLabel, String targetLabel, long count) {
        patterns.add(new RelationshipPattern(sourceLabel, targetLabel, count));
    }
    
    /**
     * 获取总的关系数量
     */
    public long getTotalCount() {
        return patterns.stream().mapToLong(RelationshipPattern::getCount).sum();
    }
    
    /**
     * 获取最常见的连接模式
     */
    public List<RelationshipPattern> getMostCommonPatterns(int limit) {
        return patterns.stream()
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(limit)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * 检查是否连接特定的节点类型
     */
    public boolean connectsNodeTypes(String sourceLabel, String targetLabel) {
        return patterns.stream()
                .anyMatch(p -> p.getSourceLabel().equals(sourceLabel) && 
                              p.getTargetLabel().equals(targetLabel));
    }
    
    @Override
    public String toString() {
        return String.format("RelationshipType{type='%s', totalCount=%d, patterns=%d}", 
                           type, getTotalCount(), patterns.size());
    }
    
    /**
     * 关系连接模式
     */
    public static class RelationshipPattern {
        private final String sourceLabel;
        private final String targetLabel;
        private final long count;
        
        public RelationshipPattern(String sourceLabel, String targetLabel, long count) {
            this.sourceLabel = sourceLabel;
            this.targetLabel = targetLabel;
            this.count = count;
        }
        
        public String getSourceLabel() { return sourceLabel; }
        public String getTargetLabel() { return targetLabel; }
        public long getCount() { return count; }
        
        @Override
        public String toString() {
            return String.format("(%s)-[:%s]->(%s) [%d]", 
                               sourceLabel, "", targetLabel, count);
        }
    }
}