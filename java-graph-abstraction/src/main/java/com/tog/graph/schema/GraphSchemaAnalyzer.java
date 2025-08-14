package com.tog.graph.schema;

import com.tog.graph.core.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图数据库Schema分析器
 * 在搜索之前分析数据库结构，为智能搜索提供指导
 */
public class GraphSchemaAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphSchemaAnalyzer.class);
    
    private final GraphDatabase graphDatabase;
    private GraphSchema cachedSchema;
    private long lastAnalysisTime = 0;
    private final long cacheValidityMs = 300000; // 5分钟缓存
    
    public GraphSchemaAnalyzer(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }
    
    /**
     * 分析图数据库的Schema
     */
    public GraphSchema analyzeSchema() {
        // 检查缓存
        if (cachedSchema != null && 
            (System.currentTimeMillis() - lastAnalysisTime) < cacheValidityMs) {
            logger.debug("Using cached schema analysis");
            return cachedSchema;
        }
        
        logger.info("Analyzing graph database schema...");
        
        try {
            GraphSchema schema = new GraphSchema();
            
            // 1. 分析节点类型和属性
            analyzeNodeTypes(schema);
            
            // 2. 分析关系类型和属性
            analyzeRelationshipTypes(schema);
            
            // 3. 分析图的统计信息
            analyzeGraphStatistics(schema);
            
            // 4. 构建搜索索引建议
            buildSearchIndexSuggestions(schema);
            
            cachedSchema = schema;
            lastAnalysisTime = System.currentTimeMillis();
            
            logger.info("Schema analysis completed: {} node types, {} relationship types", 
                       schema.getNodeTypes().size(), schema.getRelationshipTypes().size());
            
            return schema;
            
        } catch (Exception e) {
            logger.error("Failed to analyze graph schema", e);
            return createFallbackSchema();
        }
    }
    
    /**
     * 分析节点类型和属性
     */
    private void analyzeNodeTypes(GraphSchema schema) {
        try {
            // 获取所有节点标签 - 使用更兼容的方式
            List<Map<String, Object>> labelResults;
            try {
                labelResults = graphDatabase.executeQuery(
                    "CALL db.labels() YIELD label RETURN label", new HashMap<>());
            } catch (Exception e) {
                // 如果db.labels()不可用，使用备用方法
                logger.debug("db.labels() not available, using fallback method");
                labelResults = graphDatabase.executeQuery(
                    "MATCH (n) RETURN DISTINCT labels(n) as labelList", new HashMap<>());
                // 需要处理labelList格式
                List<Map<String, Object>> processedResults = new ArrayList<>();
                for (Map<String, Object> result : labelResults) {
                    @SuppressWarnings("unchecked")
                    List<String> labelList = (List<String>) result.get("labelList");
                    for (String label : labelList) {
                        Map<String, Object> labelResult = new HashMap<>();
                        labelResult.put("label", label);
                        processedResults.add(labelResult);
                    }
                }
                labelResults = processedResults;
            }
            
            for (Map<String, Object> result : labelResults) {
                String label = (String) result.get("label");
                NodeTypeInfo nodeType = new NodeTypeInfo(label);
                
                // 分析该类型节点的属性
                analyzeNodeProperties(nodeType);
                
                // 统计节点数量
                List<Map<String, Object>> countResults = graphDatabase.executeQuery(
                    "MATCH (n:" + label + ") RETURN count(n) as count", new HashMap<>());
                
                if (!countResults.isEmpty()) {
                    nodeType.setCount(((Number) countResults.get(0).get("count")).longValue());
                }
                
                schema.addNodeType(nodeType);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze node types", e);
        }
    }
    
    /**
     * 分析节点属性
     */
    private void analyzeNodeProperties(NodeTypeInfo nodeType) {
        try {
            String query = String.format(
                "MATCH (n:%s) WITH keys(n) as props " +
                "UNWIND props as prop " +
                "RETURN prop, count(*) as frequency " +
                "ORDER BY frequency DESC LIMIT 50", 
                nodeType.getLabel());
            
            List<Map<String, Object>> results = graphDatabase.executeQuery(query, new HashMap<>());
            
            for (Map<String, Object> result : results) {
                String property = (String) result.get("prop");
                long frequency = ((Number) result.get("frequency")).longValue();
                
                PropertyInfo propInfo = new PropertyInfo(property, frequency);
                
                // 分析属性值的类型和分布
                analyzePropertyValues(nodeType.getLabel(), property, propInfo);
                
                nodeType.addProperty(propInfo);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze properties for node type: " + nodeType.getLabel(), e);
        }
    }
    
    /**
     * 分析属性值的类型和分布
     */
    private void analyzePropertyValues(String nodeLabel, String property, PropertyInfo propInfo) {
        try {
            // 使用标准Neo4j函数进行类型推断，不依赖APOC
            String query = String.format(
                "MATCH (n:%s) WHERE n.%s IS NOT NULL " +
                "WITH n.%s as value " +
                "RETURN " +
                "CASE " +
                "  WHEN value =~ '^-?[0-9]+$' THEN 'INTEGER' " +
                "  WHEN value =~ '^-?[0-9]*\\.[0-9]+$' THEN 'FLOAT' " +
                "  WHEN toLower(toString(value)) IN ['true', 'false'] THEN 'BOOLEAN' " +
                "  ELSE 'STRING' " +
                "END as type, count(*) as count " +
                "ORDER BY count DESC LIMIT 10", 
                nodeLabel, property, property);
            
            List<Map<String, Object>> results = graphDatabase.executeQuery(query, new HashMap<>());
            
            for (Map<String, Object> result : results) {
                String type = (String) result.get("type");
                long count = ((Number) result.get("count")).longValue();
                propInfo.addValueType(type, count);
            }
            
            // 如果是字符串类型，分析一些样本值
            if (propInfo.getPrimaryType().equals("STRING")) {
                analyzeSampleValues(nodeLabel, property, propInfo);
            }
            
        } catch (Exception e) {
            logger.debug("Failed to analyze property values for {}:{}", nodeLabel, property);
            // 使用简单的类型推断作为fallback
            propInfo.addValueType("STRING", 1);
        }
    }
    
    /**
     * 分析字符串属性的样本值
     */
    private void analyzeSampleValues(String nodeLabel, String property, PropertyInfo propInfo) {
        try {
            String query = String.format(
                "MATCH (n:%s) WHERE n.%s IS NOT NULL " +
                "RETURN n.%s as value LIMIT 10", 
                nodeLabel, property, property);
            
            List<Map<String, Object>> results = graphDatabase.executeQuery(query, new HashMap<>());
            
            List<String> samples = results.stream()
                .map(r -> String.valueOf(r.get("value")))
                .collect(Collectors.toList());
            
            propInfo.setSampleValues(samples);
            
        } catch (Exception e) {
            logger.debug("Failed to get sample values for {}:{}", nodeLabel, property);
        }
    }
    
    /**
     * 分析关系类型和属性
     */
    private void analyzeRelationshipTypes(GraphSchema schema) {
        try {
            // 获取所有关系类型 - 使用更兼容的方式
            List<Map<String, Object>> typeResults;
            try {
                typeResults = graphDatabase.executeQuery(
                    "CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType", 
                    new HashMap<>());
            } catch (Exception e) {
                // 如果db.relationshipTypes()不可用，使用备用方法
                logger.debug("db.relationshipTypes() not available, using fallback method");
                typeResults = graphDatabase.executeQuery(
                    "MATCH ()-[r]->() RETURN DISTINCT type(r) as relationshipType LIMIT 100", 
                    new HashMap<>());
            }
            
            for (Map<String, Object> result : typeResults) {
                String relType = (String) result.get("relationshipType");
                RelationshipTypeInfo relationshipType = new RelationshipTypeInfo(relType);
                
                // 分析关系的连接模式
                analyzeRelationshipPatterns(relationshipType);
                
                // 分析关系属性
                analyzeRelationshipProperties(relationshipType);
                
                schema.addRelationshipType(relationshipType);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze relationship types", e);
        }
    }
    
    /**
     * 分析关系的连接模式
     */
    private void analyzeRelationshipPatterns(RelationshipTypeInfo relationshipType) {
        try {
            String query = String.format(
                "MATCH (a)-[r:%s]->(b) " +
                "RETURN labels(a) as sourceLabels, labels(b) as targetLabels, count(*) as count " +
                "ORDER BY count DESC LIMIT 20", 
                relationshipType.getType());
            
            List<Map<String, Object>> results = graphDatabase.executeQuery(query, new HashMap<>());
            
            for (Map<String, Object> result : results) {
                @SuppressWarnings("unchecked")
                List<String> sourceLabels = (List<String>) result.get("sourceLabels");
                @SuppressWarnings("unchecked")
                List<String> targetLabels = (List<String>) result.get("targetLabels");
                long count = ((Number) result.get("count")).longValue();
                
                String sourceLabel = sourceLabels.isEmpty() ? "Unknown" : sourceLabels.get(0);
                String targetLabel = targetLabels.isEmpty() ? "Unknown" : targetLabels.get(0);
                
                relationshipType.addPattern(sourceLabel, targetLabel, count);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze relationship patterns for: " + relationshipType.getType(), e);
        }
    }
    
    /**
     * 分析关系属性
     */
    private void analyzeRelationshipProperties(RelationshipTypeInfo relationshipType) {
        try {
            String query = String.format(
                "MATCH ()-[r:%s]->() WITH keys(r) as props " +
                "UNWIND props as prop " +
                "RETURN prop, count(*) as frequency " +
                "ORDER BY frequency DESC LIMIT 20", 
                relationshipType.getType());
            
            List<Map<String, Object>> results = graphDatabase.executeQuery(query, new HashMap<>());
            
            for (Map<String, Object> result : results) {
                String property = (String) result.get("prop");
                long frequency = ((Number) result.get("frequency")).longValue();
                
                PropertyInfo propInfo = new PropertyInfo(property, frequency);
                relationshipType.addProperty(propInfo);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze relationship properties for: " + relationshipType.getType(), e);
        }
    }
    
    /**
     * 分析图的统计信息
     */
    private void analyzeGraphStatistics(GraphSchema schema) {
        try {
            // 总节点数
            List<Map<String, Object>> nodeCountResults = graphDatabase.executeQuery(
                "MATCH (n) RETURN count(n) as totalNodes", new HashMap<>());
            
            if (!nodeCountResults.isEmpty()) {
                schema.setTotalNodes(((Number) nodeCountResults.get(0).get("totalNodes")).longValue());
            }
            
            // 总关系数
            List<Map<String, Object>> relCountResults = graphDatabase.executeQuery(
                "MATCH ()-[r]->() RETURN count(r) as totalRelationships", new HashMap<>());
            
            if (!relCountResults.isEmpty()) {
                schema.setTotalRelationships(((Number) relCountResults.get(0).get("totalRelationships")).longValue());
            }
            
            // 平均度数
            if (schema.getTotalNodes() > 0) {
                double avgDegree = (double) (schema.getTotalRelationships() * 2) / schema.getTotalNodes();
                schema.setAverageDegree(avgDegree);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze graph statistics", e);
        }
    }
    
    /**
     * 构建搜索索引建议
     */
    private void buildSearchIndexSuggestions(GraphSchema schema) {
        List<String> suggestions = new ArrayList<>();
        
        // 为高频属性建议索引
        for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
            for (PropertyInfo prop : nodeType.getProperties()) {
                if (prop.getFrequency() > nodeType.getCount() * 0.8) { // 80%以上的节点都有这个属性
                    suggestions.add(String.format("CREATE INDEX FOR (n:%s) ON (n.%s)", 
                                                 nodeType.getLabel(), prop.getName()));
                }
            }
        }
        
        schema.setIndexSuggestions(suggestions);
    }
    
    /**
     * 创建fallback schema（当分析失败时使用）
     */
    private GraphSchema createFallbackSchema() {
        GraphSchema schema = new GraphSchema();
        
        // 基于已知的数据创建基本schema
        NodeTypeInfo personType = new NodeTypeInfo("Person");
        personType.addProperty(new PropertyInfo("name", 100));
        personType.addProperty(new PropertyInfo("id", 100));
        schema.addNodeType(personType);
        
        NodeTypeInfo conceptType = new NodeTypeInfo("Concept");
        conceptType.addProperty(new PropertyInfo("name", 100));
        schema.addNodeType(conceptType);
        
        NodeTypeInfo locationType = new NodeTypeInfo("Location");
        locationType.addProperty(new PropertyInfo("name", 100));
        schema.addNodeType(locationType);
        
        RelationshipTypeInfo developedRel = new RelationshipTypeInfo("DEVELOPED");
        developedRel.addPattern("Person", "Concept", 10);
        schema.addRelationshipType(developedRel);
        
        RelationshipTypeInfo bornInRel = new RelationshipTypeInfo("BORN_IN");
        bornInRel.addPattern("Person", "Location", 10);
        schema.addRelationshipType(bornInRel);
        
        return schema;
    }
    
    /**
     * 根据问题推荐搜索策略
     */
    public SearchStrategy recommendSearchStrategy(String question, GraphSchema schema) {
        SearchStrategy strategy = new SearchStrategy();
        
        // 分析问题中的关键词
        Set<String> questionWords = Arrays.stream(question.toLowerCase().split("\\s+"))
                .collect(Collectors.toSet());
        
        // 推荐相关的节点类型
        for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
            double relevance = calculateNodeTypeRelevance(questionWords, nodeType);
            if (relevance > 0.3) {
                strategy.addRelevantNodeType(nodeType.getLabel(), relevance);
            }
        }
        
        // 推荐相关的关系类型
        for (RelationshipTypeInfo relType : schema.getRelationshipTypes()) {
            double relevance = calculateRelationshipRelevance(questionWords, relType);
            if (relevance > 0.2) {
                strategy.addRelevantRelationshipType(relType.getType(), relevance);
            }
        }
        
        // 推荐搜索属性
        recommendSearchProperties(questionWords, schema, strategy);
        
        return strategy;
    }
    
    private double calculateNodeTypeRelevance(Set<String> questionWords, NodeTypeInfo nodeType) {
        double score = 0.0;
        
        // 检查节点类型名称
        if (questionWords.contains(nodeType.getLabel().toLowerCase())) {
            score += 0.8;
        }
        
        // 检查属性样本值
        for (PropertyInfo prop : nodeType.getProperties()) {
            for (String sample : prop.getSampleValues()) {
                for (String word : questionWords) {
                    if (sample.toLowerCase().contains(word)) {
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
    
    private void recommendSearchProperties(Set<String> questionWords, GraphSchema schema, SearchStrategy strategy) {
        for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
            for (PropertyInfo prop : nodeType.getProperties()) {
                if (prop.getName().equals("name") || prop.getName().equals("title")) {
                    strategy.addSearchProperty(nodeType.getLabel(), prop.getName(), 0.9);
                } else if (questionWords.contains(prop.getName().toLowerCase())) {
                    strategy.addSearchProperty(nodeType.getLabel(), prop.getName(), 0.7);
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
}