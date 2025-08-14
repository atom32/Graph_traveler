package com.tog.graph.schema;

import java.util.*;

/**
 * 基于Schema分析的搜索策略
 */
public class SearchStrategy {
    
    private final Map<String, Double> relevantNodeTypes = new HashMap<>();
    private final Map<String, Double> relevantRelationshipTypes = new HashMap<>();
    private final Map<String, Map<String, Double>> searchProperties = new HashMap<>(); // nodeType -> property -> relevance
    private final List<String> suggestedQueries = new ArrayList<>();
    private double confidenceScore = 0.0;
    
    public void addRelevantNodeType(String nodeType, double relevance) {
        relevantNodeTypes.put(nodeType, relevance);
        updateConfidenceScore();
    }
    
    public void addRelevantRelationshipType(String relationshipType, double relevance) {
        relevantRelationshipTypes.put(relationshipType, relevance);
        updateConfidenceScore();
    }
    
    public void addSearchProperty(String nodeType, String property, double relevance) {
        searchProperties.computeIfAbsent(nodeType, k -> new HashMap<>()).put(property, relevance);
    }
    
    public void addSuggestedQuery(String query) {
        suggestedQueries.add(query);
    }
    
    public Map<String, Double> getRelevantNodeTypes() { return relevantNodeTypes; }
    public Map<String, Double> getRelevantRelationshipTypes() { return relevantRelationshipTypes; }
    public Map<String, Map<String, Double>> getSearchProperties() { return searchProperties; }
    public List<String> getSuggestedQueries() { return suggestedQueries; }
    public double getConfidenceScore() { return confidenceScore; }
    
    /**
     * 获取最相关的节点类型
     */
    public List<String> getTopNodeTypes(int limit) {
        return relevantNodeTypes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * 获取最相关的关系类型
     */
    public List<String> getTopRelationshipTypes(int limit) {
        return relevantRelationshipTypes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * 获取特定节点类型的最佳搜索属性
     */
    public List<String> getBestSearchProperties(String nodeType, int limit) {
        Map<String, Double> properties = searchProperties.get(nodeType);
        if (properties == null) return new ArrayList<>();
        
        return properties.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * 生成Cypher查询建议
     */
    public List<String> generateCypherQueries(String searchTerm) {
        List<String> queries = new ArrayList<>();
        
        // 为每个相关的节点类型生成查询
        for (String nodeType : getTopNodeTypes(3)) {
            List<String> properties = getBestSearchProperties(nodeType, 2);
            
            if (properties.isEmpty()) {
                // 如果没有特定属性，使用通用搜索
                queries.add(String.format(
                    "MATCH (n:%s) WHERE toLower(toString(n.name)) CONTAINS toLower('%s') RETURN n LIMIT 10", 
                    nodeType, searchTerm));
            } else {
                // 使用最佳属性进行搜索
                for (String property : properties) {
                    queries.add(String.format(
                        "MATCH (n:%s) WHERE toLower(toString(n.%s)) CONTAINS toLower('%s') RETURN n LIMIT 10", 
                        nodeType, property, searchTerm));
                }
            }
        }
        
        // 生成关系遍历查询
        if (!relevantRelationshipTypes.isEmpty()) {
            String topRelType = getTopRelationshipTypes(1).get(0);
            String topNodeType = getTopNodeTypes(1).get(0);
            
            queries.add(String.format(
                "MATCH (n:%s)-[r:%s]-(m) WHERE toLower(toString(n.name)) CONTAINS toLower('%s') RETURN n, r, m LIMIT 10", 
                topNodeType, topRelType, searchTerm));
        }
        
        return queries;
    }
    
    /**
     * 更新置信度分数
     */
    private void updateConfidenceScore() {
        double nodeTypeScore = relevantNodeTypes.values().stream()
                .mapToDouble(Double::doubleValue)
                .max().orElse(0.0);
        
        double relationshipScore = relevantRelationshipTypes.values().stream()
                .mapToDouble(Double::doubleValue)
                .max().orElse(0.0);
        
        this.confidenceScore = Math.max(nodeTypeScore, relationshipScore);
    }
    
    /**
     * 检查策略是否有效
     */
    public boolean isEffective() {
        return confidenceScore > 0.3 && (!relevantNodeTypes.isEmpty() || !relevantRelationshipTypes.isEmpty());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SearchStrategy{\n");
        sb.append(String.format("  confidence: %.3f\n", confidenceScore));
        sb.append("  nodeTypes: ").append(relevantNodeTypes).append("\n");
        sb.append("  relationshipTypes: ").append(relevantRelationshipTypes).append("\n");
        sb.append("  searchProperties: ").append(searchProperties).append("\n");
        sb.append("}");
        return sb.toString();
    }
}