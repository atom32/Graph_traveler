package com.tog.graph.api.dto;

import java.util.Set;

/**
 * Schema 信息响应
 */
public class SchemaResponse {
    private boolean available;
    private String summary;
    private Set<String> nodeTypes;
    private Set<String> relationshipTypes;
    private long totalNodes;
    private long totalRelationships;
    
    public SchemaResponse() {}
    
    public SchemaResponse(boolean available, String summary, Set<String> nodeTypes, 
                         Set<String> relationshipTypes, long totalNodes, long totalRelationships) {
        this.available = available;
        this.summary = summary;
        this.nodeTypes = nodeTypes;
        this.relationshipTypes = relationshipTypes;
        this.totalNodes = totalNodes;
        this.totalRelationships = totalRelationships;
    }
    
    // Getters and Setters
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public Set<String> getNodeTypes() { return nodeTypes; }
    public void setNodeTypes(Set<String> nodeTypes) { this.nodeTypes = nodeTypes; }
    
    public Set<String> getRelationshipTypes() { return relationshipTypes; }
    public void setRelationshipTypes(Set<String> relationshipTypes) { this.relationshipTypes = relationshipTypes; }
    
    public long getTotalNodes() { return totalNodes; }
    public void setTotalNodes(long totalNodes) { this.totalNodes = totalNodes; }
    
    public long getTotalRelationships() { return totalRelationships; }
    public void setTotalRelationships(long totalRelationships) { this.totalRelationships = totalRelationships; }
}