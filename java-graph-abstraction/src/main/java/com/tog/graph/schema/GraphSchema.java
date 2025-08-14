package com.tog.graph.schema;

import java.util.*;

/**
 * 图数据库Schema信息
 */
public class GraphSchema {
    
    private final Map<String, NodeTypeInfo> nodeTypes = new HashMap<>();
    private final Map<String, RelationshipTypeInfo> relationshipTypes = new HashMap<>();
    private long totalNodes = 0;
    private long totalRelationships = 0;
    private double averageDegree = 0.0;
    private List<String> indexSuggestions = new ArrayList<>();
    private long analysisTimestamp = System.currentTimeMillis();
    
    // Getters and Setters
    public Collection<NodeTypeInfo> getNodeTypes() {
        return nodeTypes.values();
    }
    
    public NodeTypeInfo getNodeType(String label) {
        return nodeTypes.get(label);
    }
    
    public void addNodeType(NodeTypeInfo nodeType) {
        nodeTypes.put(nodeType.getLabel(), nodeType);
    }
    
    public Collection<RelationshipTypeInfo> getRelationshipTypes() {
        return relationshipTypes.values();
    }
    
    public RelationshipTypeInfo getRelationshipType(String type) {
        return relationshipTypes.get(type);
    }
    
    public void addRelationshipType(RelationshipTypeInfo relationshipType) {
        relationshipTypes.put(relationshipType.getType(), relationshipType);
    }
    
    public long getTotalNodes() { return totalNodes; }
    public void setTotalNodes(long totalNodes) { this.totalNodes = totalNodes; }
    
    public long getTotalRelationships() { return totalRelationships; }
    public void setTotalRelationships(long totalRelationships) { this.totalRelationships = totalRelationships; }
    
    public double getAverageDegree() { return averageDegree; }
    public void setAverageDegree(double averageDegree) { this.averageDegree = averageDegree; }
    
    public List<String> getIndexSuggestions() { return indexSuggestions; }
    public void setIndexSuggestions(List<String> indexSuggestions) { this.indexSuggestions = indexSuggestions; }
    
    public long getAnalysisTimestamp() { return analysisTimestamp; }
    
    /**
     * 获取Schema摘要信息
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph Schema Summary:\n");
        sb.append(String.format("- Total Nodes: %,d\n", totalNodes));
        sb.append(String.format("- Total Relationships: %,d\n", totalRelationships));
        sb.append(String.format("- Average Degree: %.2f\n", averageDegree));
        sb.append(String.format("- Node Types: %d\n", nodeTypes.size()));
        sb.append(String.format("- Relationship Types: %d\n", relationshipTypes.size()));
        
        sb.append("\nNode Types:\n");
        for (NodeTypeInfo nodeType : nodeTypes.values()) {
            sb.append(String.format("  - %s (%,d nodes, %d properties)\n", 
                     nodeType.getLabel(), nodeType.getCount(), nodeType.getProperties().size()));
        }
        
        sb.append("\nRelationship Types:\n");
        for (RelationshipTypeInfo relType : relationshipTypes.values()) {
            sb.append(String.format("  - %s (%d patterns)\n", 
                     relType.getType(), relType.getPatterns().size()));
        }
        
        return sb.toString();
    }
    
    /**
     * 检查Schema是否包含特定的节点类型
     */
    public boolean hasNodeType(String label) {
        return nodeTypes.containsKey(label);
    }
    
    /**
     * 检查Schema是否包含特定的关系类型
     */
    public boolean hasRelationshipType(String type) {
        return relationshipTypes.containsKey(type);
    }
    
    /**
     * 获取最常见的节点类型
     */
    public List<NodeTypeInfo> getMostCommonNodeTypes(int limit) {
        return nodeTypes.values().stream()
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(limit)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * 获取最常见的关系类型
     */
    public List<RelationshipTypeInfo> getMostCommonRelationshipTypes(int limit) {
        return relationshipTypes.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()))
                .limit(limit)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
}