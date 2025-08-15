package com.tog.graph.schema;

import com.tog.graph.core.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库中立的Schema分析器
 * 完全基于GraphDatabase接口的抽象方法，不包含任何数据库特定代码
 */
public class DatabaseNeutralSchemaAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseNeutralSchemaAnalyzer.class);
    
    private final GraphDatabase graphDatabase;
    private GraphSchema cachedSchema;
    private long lastAnalysisTime = 0;
    private final long cacheValidityMs = 300000; // 5分钟缓存
    
    public DatabaseNeutralSchemaAnalyzer(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }
    
    /**
     * 分析图数据库的Schema - 完全数据库中立
     */
    public GraphSchema analyzeSchema() {
        // 检查缓存
        if (cachedSchema != null && 
            (System.currentTimeMillis() - lastAnalysisTime) < cacheValidityMs) {
            logger.debug("Using cached schema analysis");
            return cachedSchema;
        }
        
        logger.info("Analyzing graph database schema using database-neutral methods...");
        
        try {
            GraphSchema schema = new GraphSchema();
            
            // 1. 使用抽象接口分析节点类型
            analyzeNodeTypesNeutral(schema);
            
            // 2. 使用抽象接口分析关系类型
            analyzeRelationshipTypesNeutral(schema);
            
            // 3. 使用抽象接口分析统计信息
            analyzeGraphStatisticsNeutral(schema);
            
            // 4. 构建搜索策略建议
            buildSearchStrategiesNeutral(schema);
            
            cachedSchema = schema;
            lastAnalysisTime = System.currentTimeMillis();
            
            logger.info("Database-neutral schema analysis completed: {} node types, {} relationship types", 
                       schema.getNodeTypes().size(), schema.getRelationshipTypes().size());
            
            return schema;
            
        } catch (Exception e) {
            logger.error("Failed to analyze graph schema", e);
            return createFallbackSchema();
        }
    }
    
    /**
     * 使用数据库中立方法分析节点类型
     */
    private void analyzeNodeTypesNeutral(GraphSchema schema) {
        try {
            // 使用抽象接口获取所有节点类型
            List<String> nodeTypes = graphDatabase.getAllNodeTypes();
            
            for (String nodeType : nodeTypes) {
                NodeTypeInfo nodeTypeInfo = new NodeTypeInfo(nodeType);
                
                // 使用抽象接口获取节点数量
                long count = graphDatabase.getNodeCount(nodeType);
                nodeTypeInfo.setCount(count);
                
                // 使用抽象接口分析节点属性
                List<PropertyInfo> properties = graphDatabase.analyzeNodeProperties(nodeType);
                for (PropertyInfo propInfo : properties) {
                    nodeTypeInfo.addProperty(propInfo);
                    
                    // 获取属性样本值
                    List<String> sampleValues = graphDatabase.getSamplePropertyValues(
                        nodeType, propInfo.getName(), 10);
                    propInfo.setSampleValues(sampleValues);
                }
                
                schema.addNodeType(nodeTypeInfo);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze node types using neutral methods", e);
        }
    }
    
    /**
     * 使用数据库中立方法分析关系类型
     */
    private void analyzeRelationshipTypesNeutral(GraphSchema schema) {
        try {
            // 使用抽象接口获取所有关系类型
            List<String> relationshipTypes = graphDatabase.getAllRelationshipTypes();
            
            for (String relType : relationshipTypes) {
                RelationshipTypeInfo relTypeInfo = new RelationshipTypeInfo(relType);
                
                // 使用抽象接口获取关系数量
                long count = graphDatabase.getRelationshipCount(relType);
                relTypeInfo.setTotalCount(count);
                
                // 使用抽象接口分析关系属性
                List<PropertyInfo> properties = graphDatabase.analyzeRelationshipProperties(relType);
                for (PropertyInfo propInfo : properties) {
                    relTypeInfo.addProperty(propInfo);
                }
                
                // 分析关系连接模式（需要额外的抽象方法支持）
                analyzeRelationshipPatternsNeutral(relTypeInfo);
                
                schema.addRelationshipType(relTypeInfo);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze relationship types using neutral methods", e);
        }
    }
    
    /**
     * 使用数据库中立方法分析关系连接模式
     */
    private void analyzeRelationshipPatternsNeutral(RelationshipTypeInfo relationshipType) {
        try {
            // 这里需要GraphDatabase接口提供更多抽象方法
            // 例如：getRelationshipPatterns(String relationshipType)
            
            // 暂时使用简化实现
            relationshipType.addPattern("Unknown", "Unknown", 1);
            
        } catch (Exception e) {
            logger.debug("Failed to analyze relationship patterns for: " + relationshipType.getType(), e);
        }
    }
    
    /**
     * 使用数据库中立方法分析统计信息
     */
    private void analyzeGraphStatisticsNeutral(GraphSchema schema) {
        try {
            // 使用抽象接口获取统计信息
            long totalNodes = graphDatabase.getTotalNodeCount();
            long totalRelationships = graphDatabase.getTotalRelationshipCount();
            
            schema.setTotalNodes(totalNodes);
            schema.setTotalRelationships(totalRelationships);
            
            // 计算平均度数
            if (totalNodes > 0) {
                double avgDegree = (double) (totalRelationships * 2) / totalNodes;
                schema.setAverageDegree(avgDegree);
            }
            
            // 获取类型分布
            Map<String, Long> nodeTypeDistribution = graphDatabase.getNodeTypeDistribution();
            Map<String, Long> relationshipTypeDistribution = graphDatabase.getRelationshipTypeDistribution();
            
            schema.setNodeTypeDistribution(nodeTypeDistribution);
            schema.setRelationshipTypeDistribution(relationshipTypeDistribution);
            
        } catch (Exception e) {
            logger.warn("Failed to analyze graph statistics using neutral methods", e);
        }
    }
    
    /**
     * 构建数据库中立的搜索策略
     */
    private void buildSearchStrategiesNeutral(GraphSchema schema) {
        List<String> suggestions = new ArrayList<>();
        
        // 基于节点类型和属性频率生成建议
        for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
            for (PropertyInfo prop : nodeType.getProperties()) {
                double coverage = (double) prop.getFrequency() / nodeType.getCount();
                
                if (coverage > 0.8) { // 80%以上的节点都有这个属性
                    suggestions.add(String.format(
                        "Consider indexing %s.%s (coverage: %.1f%%)", 
                        nodeType.getLabel(), prop.getName(), coverage * 100));
                }
            }
        }
        
        schema.setIndexSuggestions(suggestions);
    }
    
    /**
     * 创建fallback schema
     */
    private GraphSchema createFallbackSchema() {
        GraphSchema schema = new GraphSchema();
        
        // 创建通用的fallback schema
        NodeTypeInfo genericNode = new NodeTypeInfo("Entity");
        genericNode.addProperty(new PropertyInfo("id", 100));
        genericNode.addProperty(new PropertyInfo("name", 100));
        schema.addNodeType(genericNode);
        
        RelationshipTypeInfo genericRel = new RelationshipTypeInfo("RELATED_TO");
        genericRel.addPattern("Entity", "Entity", 10);
        schema.addRelationshipType(genericRel);
        
        return schema;
    }
    
    /**
     * 根据问题推荐搜索策略 - 数据库中立
     */
    public SearchStrategy recommendSearchStrategy(String question, GraphSchema schema) {
        SearchStrategy strategy = new SearchStrategy();
        
        // 分析问题中的关键词
        Set<String> questionWords = Arrays.stream(question.toLowerCase().split("\\s+"))
                .collect(Collectors.toSet());
        
        // 基于Schema推荐相关的节点类型
        for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
            double relevance = calculateNodeTypeRelevance(questionWords, nodeType);
            if (relevance > 0.3) {
                strategy.addRelevantNodeType(nodeType.getLabel(), relevance);
            }
        }
        
        // 基于Schema推荐相关的关系类型
        for (RelationshipTypeInfo relType : schema.getRelationshipTypes()) {
            double relevance = calculateRelationshipRelevance(questionWords, relType);
            if (relevance > 0.2) {
                strategy.addRelevantRelationshipType(relType.getType(), relevance);
            }
        }
        
        // 推荐搜索属性
        recommendSearchPropertiesNeutral(questionWords, schema, strategy);
        
        return strategy;
    }
    
    private double calculateNodeTypeRelevance(Set<String> questionWords, NodeTypeInfo nodeType) {
        double score = 0.0;
        
        // 检查节点类型名称
        String nodeTypeLower = nodeType.getLabel().toLowerCase();
        for (String word : questionWords) {
            if (nodeTypeLower.contains(word)) {
                score += 0.8;
            }
        }
        
        // 检查属性样本值
        for (PropertyInfo prop : nodeType.getProperties()) {
            for (String sample : prop.getSampleValues()) {
                String sampleLower = sample.toLowerCase();
                for (String word : questionWords) {
                    if (sampleLower.contains(word)) {
                        score += 0.2;
                    }
                }
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    private double calculateRelationshipRelevance(Set<String> questionWords, RelationshipTypeInfo relType) {
        double score = 0.0;
        
        String relTypeLower = relType.getType().toLowerCase().replace("_", " ");
        for (String word : questionWords) {
            if (relTypeLower.contains(word)) {
                score += 0.5;
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    private void recommendSearchPropertiesNeutral(Set<String> questionWords, GraphSchema schema, SearchStrategy strategy) {
        for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
            for (PropertyInfo prop : nodeType.getProperties()) {
                double relevance = 0.0;
                
                // 通用高价值属性
                if (prop.getName().toLowerCase().matches(".*name.*|.*title.*|.*label.*")) {
                    relevance = 0.9;
                } else if (questionWords.contains(prop.getName().toLowerCase())) {
                    relevance = 0.7;
                } else {
                    // 基于属性覆盖率
                    double coverage = (double) prop.getFrequency() / nodeType.getCount();
                    relevance = coverage * 0.5;
                }
                
                if (relevance > 0.3) {
                    strategy.addSearchProperty(nodeType.getLabel(), prop.getName(), relevance);
                }
            }
        }
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        cachedSchema = null;
        lastAnalysisTime = 0;
    }
    
    /**
     * 获取数据库类型信息
     */
    public String getDatabaseType() {
        return graphDatabase.getDatabaseType();
    }
    
    /**
     * 获取数据库版本信息
     */
    public String getDatabaseVersion() {
        return graphDatabase.getVersion();
    }
}