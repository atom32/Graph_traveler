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
    private Map<String, Long> nodeTypeDistribution = new HashMap<>();
    private Map<String, Long> relationshipTypeDistribution = new HashMap<>();
    
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
    
    public Map<String, Long> getNodeTypeDistribution() { return nodeTypeDistribution; }
    public void setNodeTypeDistribution(Map<String, Long> nodeTypeDistribution) { 
        this.nodeTypeDistribution = nodeTypeDistribution; 
    }
    
    public Map<String, Long> getRelationshipTypeDistribution() { return relationshipTypeDistribution; }
    public void setRelationshipTypeDistribution(Map<String, Long> relationshipTypeDistribution) { 
        this.relationshipTypeDistribution = relationshipTypeDistribution; 
    }
    
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
    
    // ========== 智能抽取支持方法 ==========
    
    private Map<String, Double> relationTypeWeights = new HashMap<>();
    private List<String> highPriorityRelationKeywords = new ArrayList<>();
    private List<String> mediumPriorityRelationKeywords = new ArrayList<>();
    private List<String> lowPriorityRelationKeywords = new ArrayList<>();
    private List<String> stopWords = new ArrayList<>();
    private Map<String, List<String>> entityExtractionPatterns = new HashMap<>();
    private Map<String, List<String>> entityTypeInferenceRules = new HashMap<>();
    private Map<String, Double> entityConfidenceRules = new HashMap<>();
    private Map<String, List<String>> entityTypeSearchProperties = new HashMap<>();
    private Map<String, List<String>> queryIntentPatterns = new HashMap<>();
    
    /**
     * 获取关系类型权重配置
     */
    public Map<String, Double> getRelationTypeWeights() {
        return relationTypeWeights;
    }
    
    public void setRelationTypeWeights(Map<String, Double> relationTypeWeights) {
        this.relationTypeWeights = relationTypeWeights;
    }
    
    /**
     * 获取高优先级关系关键词
     */
    public List<String> getHighPriorityRelationKeywords() {
        return highPriorityRelationKeywords;
    }
    
    public void setHighPriorityRelationKeywords(List<String> keywords) {
        this.highPriorityRelationKeywords = keywords;
    }
    
    /**
     * 获取中优先级关系关键词
     */
    public List<String> getMediumPriorityRelationKeywords() {
        return mediumPriorityRelationKeywords;
    }
    
    public void setMediumPriorityRelationKeywords(List<String> keywords) {
        this.mediumPriorityRelationKeywords = keywords;
    }
    
    /**
     * 获取低优先级关系关键词
     */
    public List<String> getLowPriorityRelationKeywords() {
        return lowPriorityRelationKeywords;
    }
    
    public void setLowPriorityRelationKeywords(List<String> keywords) {
        this.lowPriorityRelationKeywords = keywords;
    }
    
    /**
     * 获取停用词列表
     */
    public List<String> getStopWords() {
        return stopWords;
    }
    
    public void setStopWords(List<String> stopWords) {
        this.stopWords = stopWords;
    }
    
    /**
     * 获取实体抽取模式
     */
    public Map<String, List<String>> getEntityExtractionPatterns() {
        return entityExtractionPatterns;
    }
    
    public void setEntityExtractionPatterns(Map<String, List<String>> patterns) {
        this.entityExtractionPatterns = patterns;
    }
    
    /**
     * 获取实体类型推断规则
     */
    public Map<String, List<String>> getEntityTypeInferenceRules() {
        return entityTypeInferenceRules;
    }
    
    public void setEntityTypeInferenceRules(Map<String, List<String>> rules) {
        this.entityTypeInferenceRules = rules;
    }
    
    /**
     * 获取实体置信度规则
     */
    public Map<String, Double> getEntityConfidenceRules() {
        return entityConfidenceRules;
    }
    
    public void setEntityConfidenceRules(Map<String, Double> rules) {
        this.entityConfidenceRules = rules;
    }
    
    /**
     * 获取实体类型搜索属性映射
     */
    public Map<String, List<String>> getEntityTypeSearchProperties() {
        return entityTypeSearchProperties;
    }
    
    public void setEntityTypeSearchProperties(Map<String, List<String>> properties) {
        this.entityTypeSearchProperties = properties;
    }
    
    /**
     * 获取查询意图模式
     */
    public Map<String, List<String>> getQueryIntentPatterns() {
        return queryIntentPatterns;
    }
    
    public void setQueryIntentPatterns(Map<String, List<String>> patterns) {
        this.queryIntentPatterns = patterns;
    }
    
    /**
     * 获取所有节点类型名称
     */
    public Set<String> getNodeTypeNames() {
        return nodeTypes.keySet();
    }
    
    /**
     * 初始化默认的智能抽取配置
     */
    public void initializeDefaultExtractionConfig() {
        // 设置默认的关系优先级关键词
        highPriorityRelationKeywords.addAll(Arrays.asList("主治", "治疗", "包含", "组成"));
        mediumPriorityRelationKeywords.addAll(Arrays.asList("相关", "影响", "对应"));
        lowPriorityRelationKeywords.addAll(Arrays.asList("提及", "描述"));
        
        // 设置默认停用词
        stopWords.addAll(Arrays.asList("的", "是", "在", "了", "和", "与", "为", "中", 
                                      "什么", "怎么", "哪里", "为什么", "这个", "那个"));
        
        // 设置默认实体抽取模式
        entityExtractionPatterns.put("Person", Arrays.asList("([\\u4e00-\\u9fa5]{2,4})"));
        entityExtractionPatterns.put("Book", Arrays.asList("《([^》]+)》"));
        
        // 设置默认类型推断规则
        entityTypeInferenceRules.put("Person", Arrays.asList("[\\u4e00-\\u9fa5]{2,4}"));
        entityTypeInferenceRules.put("Book", Arrays.asList(".*[经论方]$"));
        
        // 设置默认置信度规则
        entityConfidenceRules.put(".*[经论方汤丸散].*", 0.3);
        entityConfidenceRules.put("[\\u4e00-\\u9fa5]{2,4}", 0.2);
        
        // 设置默认搜索属性
        entityTypeSearchProperties.put("Person", Arrays.asList("name", "姓名", "人名", "字", "号"));
        entityTypeSearchProperties.put("Book", Arrays.asList("name", "title", "书名", "全称"));
        
        // 设置默认查询意图模式
        queryIntentPatterns.put("关系分析", Arrays.asList("关系", "联系", "连接"));
        queryIntentPatterns.put("信息查询", Arrays.asList("是什么", "介绍", "详情"));
        queryIntentPatterns.put("治疗方案", Arrays.asList("治疗", "怎么办", "如何"));
    }
}