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
     * 安全转义属性名，处理特殊字符和数字开头的情况
     */
    private String escapePropertyName(String propertyName) {
        if (propertyName == null || propertyName.trim().isEmpty()) {
            return propertyName;
        }
        
        // 检查是否需要转义：包含特殊字符、以数字开头、或包含空格
        if (propertyName.matches(".*[^a-zA-Z0-9_].*") || 
            Character.isDigit(propertyName.charAt(0)) ||
            propertyName.contains(" ")) {
            return "`" + propertyName.replace("`", "``") + "`";
        }
        
        return propertyName;
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
        long startTime = System.currentTimeMillis();
        
        try {
            GraphSchema schema = new GraphSchema();
            
            // 1. 分析节点类型和属性
            logger.info("Step 1/4: Analyzing node types...");
            analyzeNodeTypes(schema);
            logger.info("Step 1/4 completed in {}ms", System.currentTimeMillis() - startTime);
            
            // 2. 分析关系类型和属性
            logger.info("Step 2/4: Analyzing relationship types...");
            long step2Start = System.currentTimeMillis();
            analyzeRelationshipTypes(schema);
            logger.info("Step 2/4 completed in {}ms", System.currentTimeMillis() - step2Start);
            
            // 3. 分析图的统计信息
            logger.info("Step 3/4: Analyzing graph statistics...");
            long step3Start = System.currentTimeMillis();
            analyzeGraphStatistics(schema);
            logger.info("Step 3/4 completed in {}ms", System.currentTimeMillis() - step3Start);
            
            // 4. 构建搜索索引建议
            logger.info("Step 4/4: Building search index suggestions...");
            long step4Start = System.currentTimeMillis();
            buildSearchIndexSuggestions(schema);
            logger.info("Step 4/4 completed in {}ms", System.currentTimeMillis() - step4Start);
            
            cachedSchema = schema;
            lastAnalysisTime = System.currentTimeMillis();
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Schema analysis completed in {}ms: {} node types, {} relationship types", 
                       totalTime, schema.getNodeTypes().size(), schema.getRelationshipTypes().size());
            
            return schema;
            
        } catch (Exception e) {
            logger.error("Failed to analyze graph schema after {}ms", System.currentTimeMillis() - startTime, e);
            return createFallbackSchema();
        }
    }
    
    /**
     * 分析节点类型和属性
     */
    private void analyzeNodeTypes(GraphSchema schema) {
        try {
            logger.info("Starting node type analysis...");
            
            // 获取所有节点标签 - 使用更兼容的方式
            List<Map<String, Object>> labelResults;
            try {
                logger.debug("Trying db.labels()...");
                labelResults = graphDatabase.executeQuery(
                    "CALL db.labels() YIELD label RETURN label LIMIT 50", new HashMap<>());
                logger.debug("Found {} labels using db.labels()", labelResults.size());
            } catch (Exception e) {
                // 如果db.labels()不可用，使用备用方法
                logger.debug("db.labels() not available, using fallback method");
                labelResults = graphDatabase.executeQuery(
                    "MATCH (n) RETURN DISTINCT labels(n) as labelList LIMIT 50", new HashMap<>());
                // 需要处理labelList格式
                List<Map<String, Object>> processedResults = new ArrayList<>();
                for (Map<String, Object> result : labelResults) {
                    @SuppressWarnings("unchecked")
                    List<String> labelList = (List<String>) result.get("labelList");
                    if (labelList != null) {
                        for (String label : labelList) {
                            if (label != null && !label.trim().isEmpty()) {
                                Map<String, Object> labelResult = new HashMap<>();
                                labelResult.put("label", label);
                                processedResults.add(labelResult);
                            }
                        }
                    }
                }
                labelResults = processedResults;
                logger.debug("Found {} labels using fallback method", labelResults.size());
            }
            
            int processedCount = 0;
            for (Map<String, Object> result : labelResults) {
                String label = (String) result.get("label");
                if (label == null || label.trim().isEmpty()) {
                    continue;
                }
                
                logger.debug("Analyzing node type: {}", label);
                NodeTypeInfo nodeType = new NodeTypeInfo(label);
                
                try {
                    // 统计节点数量（先做这个，因为比较快）
                    List<Map<String, Object>> countResults = graphDatabase.executeQuery(
                        "MATCH (n:`" + label + "`) RETURN count(n) as count", new HashMap<>());
                    
                    if (!countResults.isEmpty()) {
                        long count = ((Number) countResults.get(0).get("count")).longValue();
                        nodeType.setCount(count);
                        logger.debug("Node type {} has {} nodes", label, count);
                        
                        // 只分析有节点的类型的属性
                        if (count > 0) {
                            analyzeNodeProperties(nodeType);
                        }
                    }
                    
                    schema.addNodeType(nodeType);
                    processedCount++;
                    
                } catch (Exception e) {
                    logger.warn("Failed to analyze node type: {}", label, e);
                    // 即使失败也添加基本信息
                    schema.addNodeType(nodeType);
                }
            }
            
            logger.info("Completed node type analysis: {} types processed", processedCount);
            
        } catch (Exception e) {
            logger.error("Failed to analyze node types", e);
            // 添加一个基本的fallback节点类型
            NodeTypeInfo fallbackType = new NodeTypeInfo("Unknown");
            fallbackType.setCount(0);
            schema.addNodeType(fallbackType);
        }
    }
    
    /**
     * 分析节点属性
     */
    private void analyzeNodeProperties(NodeTypeInfo nodeType) {
        try {
            logger.debug("Analyzing properties for node type: {}", nodeType.getLabel());
            
            String query = String.format(
                "MATCH (n:`%s`) WITH keys(n) as props " +
                "UNWIND props as prop " +
                "RETURN prop, count(*) as frequency " +
                "ORDER BY frequency DESC LIMIT 20", 
                nodeType.getLabel());
            
            List<Map<String, Object>> results = graphDatabase.executeQuery(query, new HashMap<>());
            logger.debug("Found {} properties for node type: {}", results.size(), nodeType.getLabel());
            
            int propCount = 0;
            for (Map<String, Object> result : results) {
                String property = (String) result.get("prop");
                if (property == null || property.trim().isEmpty()) {
                    continue;
                }
                
                long frequency = ((Number) result.get("frequency")).longValue();
                PropertyInfo propInfo = new PropertyInfo(property, frequency);
                
                try {
                    // 只对高频属性进行详细分析，避免性能问题
                    if (frequency > nodeType.getCount() * 0.1 || propCount < 5) {
                        analyzePropertyValues(nodeType.getLabel(), property, propInfo);
                    } else {
                        // 对低频属性只做基本类型推断
                        propInfo.addValueType("STRING", frequency);
                    }
                } catch (Exception e) {
                    logger.debug("Failed to analyze property values for {}:{}", nodeType.getLabel(), property, e);
                    propInfo.addValueType("UNKNOWN", frequency);
                }
                
                nodeType.addProperty(propInfo);
                propCount++;
            }
            
            logger.debug("Analyzed {} properties for node type: {}", propCount, nodeType.getLabel());
            
        } catch (Exception e) {
            logger.warn("Failed to analyze properties for node type: {}", nodeType.getLabel(), e);
        }
    }
    
    /**
     * 分析属性值的类型和分布
     */
    private void analyzePropertyValues(String nodeLabel, String property, PropertyInfo propInfo) {
        try {
            logger.debug("Analyzing property values for {}:{}", nodeLabel, property);
            
            // 安全转义属性名
            String escapedProperty = escapePropertyName(property);
            
            // 使用最简单安全的方法获取样本值来判断类型
            String sampleQuery = String.format(
                "MATCH (n:`%s`) WHERE n.%s IS NOT NULL " +
                "RETURN n.%s as value LIMIT 3", 
                nodeLabel, escapedProperty, escapedProperty);
            
            List<Map<String, Object>> sampleResults = graphDatabase.executeQuery(sampleQuery, new HashMap<>());
            
            if (sampleResults.isEmpty()) {
                propInfo.addValueType("NULL", 1);
                return;
            }
            
            // 基于样本值推断类型
            Map<String, Integer> typeCount = new HashMap<>();
            List<String> samples = new ArrayList<>();
            
            for (Map<String, Object> result : sampleResults) {
                Object value = result.get("value");
                String type = inferValueType(value);
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
                
                // 收集样本值
                if (samples.size() < 3) {
                    samples.add(formatSampleValue(value));
                }
            }
            
            // 添加类型信息
            for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                propInfo.addValueType(entry.getKey(), entry.getValue());
            }
            
            // 设置样本值
            propInfo.setSampleValues(samples);
            
            logger.debug("Property {}:{} analyzed - types: {}", nodeLabel, property, typeCount.keySet());
            
        } catch (Exception e) {
            logger.debug("Failed to analyze property values for {}:{}, using fallback", nodeLabel, property, e);
            propInfo.addValueType("STRING", 1);
        }
    }
    
    /**
     * 推断值的类型
     */
    private String inferValueType(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else if (value instanceof Integer || value instanceof Long) {
            return "INTEGER";
        } else if (value instanceof Float || value instanceof Double) {
            return "FLOAT";
        } else if (value instanceof List) {
            return "ARRAY";
        } else if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.matches("^-?[0-9]+$")) {
                return "INTEGER";
            } else if (strValue.matches("^-?[0-9]*\\.[0-9]+$")) {
                return "FLOAT";
            } else if ("true".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)) {
                return "BOOLEAN";
            } else {
                return "STRING";
            }
        } else {
            return "OBJECT";
        }
    }
    
    /**
     * 格式化样本值用于显示
     */
    private String formatSampleValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> listValue = (List<Object>) value;
            if (listValue.size() <= 3) {
                return listValue.toString();
            } else {
                return "[" + listValue.get(0) + ", " + listValue.get(1) + ", ...]";
            }
        } else {
            String strValue = String.valueOf(value);
            return strValue.length() > 50 ? strValue.substring(0, 47) + "..." : strValue;
        }
    }
    
    /**
     * 分析未知类型的属性
     */
    private void analyzeUnknownPropertyType(String nodeLabel, String property, PropertyInfo propInfo, long count) {
        try {
            // 获取一个样本值来判断类型
            String sampleQuery = String.format(
                "MATCH (n:%s) WHERE n.%s IS NOT NULL " +
                "RETURN n.%s as value LIMIT 1", 
                nodeLabel, property, property);
            
            List<Map<String, Object>> sampleResults = graphDatabase.executeQuery(sampleQuery, new HashMap<>());
            
            if (!sampleResults.isEmpty()) {
                Object sampleValue = sampleResults.get(0).get("value");
                
                if (sampleValue instanceof List) {
                    propInfo.addValueType("ARRAY", count);
                } else if (sampleValue instanceof Number) {
                    if (sampleValue instanceof Integer || sampleValue instanceof Long) {
                        propInfo.addValueType("INTEGER", count);
                    } else {
                        propInfo.addValueType("FLOAT", count);
                    }
                } else if (sampleValue instanceof String) {
                    String strValue = (String) sampleValue;
                    if (strValue.matches("^-?[0-9]+$")) {
                        propInfo.addValueType("INTEGER", count);
                    } else if (strValue.matches("^-?[0-9]*\\.[0-9]+$")) {
                        propInfo.addValueType("FLOAT", count);
                    } else {
                        propInfo.addValueType("STRING", count);
                    }
                } else {
                    propInfo.addValueType("STRING", count);
                }
            } else {
                propInfo.addValueType("STRING", count);
            }
            
        } catch (Exception e) {
            logger.debug("Failed to analyze unknown property type for {}:{}", nodeLabel, property, e);
            propInfo.addValueType("STRING", count);
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
            
            List<String> samples = new ArrayList<>();
            for (Map<String, Object> result : results) {
                Object value = result.get("value");
                if (value != null) {
                    try {
                        // 处理数组类型
                        if (value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> listValue = (List<Object>) value;
                            samples.add(listValue.toString()); // 将数组转换为字符串表示
                        } else {
                            samples.add(String.valueOf(value));
                        }
                    } catch (Exception e) {
                        // 如果转换失败，使用安全的字符串表示
                        samples.add("[Complex Value]");
                    }
                }
            }
            
            propInfo.setSampleValues(samples);
            
        } catch (Exception e) {
            logger.debug("Failed to get sample values for {}:{}", nodeLabel, property, e);
        }
    }
    
    /**
     * 分析关系类型和属性
     */
    private void analyzeRelationshipTypes(GraphSchema schema) {
        try {
            logger.info("Starting relationship type analysis...");
            
            // 获取所有关系类型 - 使用更兼容的方式
            List<Map<String, Object>> typeResults;
            try {
                logger.debug("Trying db.relationshipTypes()...");
                typeResults = graphDatabase.executeQuery(
                    "CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType LIMIT 50", 
                    new HashMap<>());
                logger.debug("Found {} relationship types using db.relationshipTypes()", typeResults.size());
            } catch (Exception e) {
                // 如果db.relationshipTypes()不可用，使用备用方法
                logger.debug("db.relationshipTypes() not available, using fallback method");
                typeResults = graphDatabase.executeQuery(
                    "MATCH ()-[r]->() RETURN DISTINCT type(r) as relationshipType LIMIT 50", 
                    new HashMap<>());
                logger.debug("Found {} relationship types using fallback method", typeResults.size());
            }
            
            int processedCount = 0;
            for (Map<String, Object> result : typeResults) {
                String relType = (String) result.get("relationshipType");
                if (relType == null || relType.trim().isEmpty()) {
                    continue;
                }
                
                logger.debug("Analyzing relationship type: {}", relType);
                RelationshipTypeInfo relationshipType = new RelationshipTypeInfo(relType);
                
                try {
                    // 只分析连接模式，跳过属性分析以提高性能
                    analyzeRelationshipPatterns(relationshipType);
                    schema.addRelationshipType(relationshipType);
                    processedCount++;
                } catch (Exception e) {
                    logger.warn("Failed to analyze relationship type: {}", relType, e);
                    // 即使失败也添加基本信息
                    schema.addRelationshipType(relationshipType);
                }
            }
            
            logger.info("Completed relationship type analysis: {} types processed", processedCount);
            
        } catch (Exception e) {
            logger.error("Failed to analyze relationship types", e);
        }
    }
    
    /**
     * 分析关系的连接模式
     */
    private void analyzeRelationshipPatterns(RelationshipTypeInfo relationshipType) {
        try {
            logger.debug("Analyzing patterns for relationship type: {}", relationshipType.getType());
            
            String query = String.format(
                "MATCH (a)-[r:`%s`]->(b) " +
                "RETURN labels(a) as sourceLabels, labels(b) as targetLabels, count(*) as count " +
                "ORDER BY count DESC LIMIT 5", 
                relationshipType.getType());
            
            List<Map<String, Object>> results = graphDatabase.executeQuery(query, new HashMap<>());
            logger.debug("Found {} patterns for relationship type: {}", results.size(), relationshipType.getType());
            
            for (Map<String, Object> result : results) {
                @SuppressWarnings("unchecked")
                List<String> sourceLabels = (List<String>) result.get("sourceLabels");
                @SuppressWarnings("unchecked")
                List<String> targetLabels = (List<String>) result.get("targetLabels");
                long count = ((Number) result.get("count")).longValue();
                
                String sourceLabel = (sourceLabels != null && !sourceLabels.isEmpty()) ? sourceLabels.get(0) : "Unknown";
                String targetLabel = (targetLabels != null && !targetLabels.isEmpty()) ? targetLabels.get(0) : "Unknown";
                
                relationshipType.addPattern(sourceLabel, targetLabel, count);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze relationship patterns for: {}", relationshipType.getType(), e);
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
        long totalNodes = 0;
        logger.debug("Analyzing graph statistics...");
        // 节点统计
        try {
            List<Map<String, Object>> nodeCountResults = graphDatabase.executeQuery(
                "MATCH (n) RETURN count(n) as totalNodes LIMIT 1", new HashMap<>());
            if (!nodeCountResults.isEmpty()) {
                totalNodes = ((Number) nodeCountResults.get(0).get("totalNodes")).longValue();
            }
            schema.setTotalNodes(totalNodes);
            logger.debug("Total nodes (direct count): {}", totalNodes);
        } catch (Exception e) {
            logger.debug("Failed to get total node count", e);
            schema.setTotalNodes(0);
        }
        // 关系统计
        try {
            List<Map<String, Object>> relCountResults = graphDatabase.executeQuery(
                "MATCH ()-[r]->() RETURN count(r) as totalRelationships LIMIT 1", new HashMap<>());
            if (!relCountResults.isEmpty()) {
                long totalRels = ((Number) relCountResults.get(0).get("totalRelationships")).longValue();
                schema.setTotalRelationships(totalRels);
                logger.debug("Total relationships: {}", totalRels);
                if (totalNodes > 0) {
                    double avgDegree = (double) (totalRels * 2) / totalNodes;
                    schema.setAverageDegree(avgDegree);
                    logger.debug("Average degree: {}", avgDegree);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get relationship statistics", e);
            schema.setTotalRelationships(0);
            schema.setAverageDegree(0.0);
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