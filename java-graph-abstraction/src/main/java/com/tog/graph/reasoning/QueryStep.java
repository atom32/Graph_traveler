package com.tog.graph.reasoning;

import java.util.*;

/**
 * 查询计划中的单个步骤
 */
public class QueryStep {
    
    public enum StepType {
        ENTITY_SEARCH,           // 实体搜索
        RELATIONSHIP_TRAVERSAL,  // 关系遍历
        PROPERTY_FILTER,         // 属性过滤
        AGGREGATION             // 聚合操作
    }
    
    private StepType stepType;
    private String targetEntityType;
    private String relationshipType;
    private String searchText;
    private List<String> searchProperties = new ArrayList<>();
    private Map<String, Object> filters = new HashMap<>();
    private double confidence = 0.0;
    private String description = "";
    
    // Getters and Setters
    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }
    
    public String getTargetEntityType() { return targetEntityType; }
    public void setTargetEntityType(String targetEntityType) { this.targetEntityType = targetEntityType; }
    
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    
    public List<String> getSearchProperties() { return searchProperties; }
    public void setSearchProperties(List<String> searchProperties) { this.searchProperties = searchProperties; }
    
    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    /**
     * 生成步骤的Cypher查询片段
     */
    public String toCypherFragment() {
        switch (stepType) {
            case ENTITY_SEARCH:
                if (searchProperties.isEmpty()) {
                    return String.format("MATCH (n:%s) WHERE toLower(n.name) CONTAINS toLower('%s')", 
                                       targetEntityType, searchText);
                } else {
                    String propertyConditions = searchProperties.stream()
                            .map(prop -> String.format("toLower(n.%s) CONTAINS toLower('%s')", prop, searchText))
                            .reduce((a, b) -> a + " OR " + b)
                            .orElse("");
                    return String.format("MATCH (n:%s) WHERE %s", targetEntityType, propertyConditions);
                }
                
            case RELATIONSHIP_TRAVERSAL:
                return String.format("-[r:%s]-", relationshipType);
                
            case PROPERTY_FILTER:
                // 实现属性过滤逻辑
                return "";
                
            case AGGREGATION:
                // 实现聚合逻辑
                return "";
                
            default:
                return "";
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(stepType).append("{");
        
        switch (stepType) {
            case ENTITY_SEARCH:
                sb.append("entity=").append(targetEntityType);
                sb.append(", text='").append(searchText).append("'");
                if (!searchProperties.isEmpty()) {
                    sb.append(", props=").append(searchProperties);
                }
                break;
                
            case RELATIONSHIP_TRAVERSAL:
                sb.append("rel=").append(relationshipType);
                break;
        }
        
        sb.append(", confidence=").append(String.format("%.3f", confidence));
        sb.append("}");
        
        return sb.toString();
    }
}