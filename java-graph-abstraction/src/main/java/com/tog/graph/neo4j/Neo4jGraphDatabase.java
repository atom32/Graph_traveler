package com.tog.graph.neo4j;

import com.tog.graph.core.*;
import com.tog.graph.schema.PropertyInfo;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j图数据库实现
 */
public class Neo4jGraphDatabase implements com.tog.graph.core.GraphDatabase {
    
    private static final Logger logger = LoggerFactory.getLogger(Neo4jGraphDatabase.class);
    
    private Driver driver;
    
    public void connect(String uri, String username, String password) {
        try {
            this.driver = org.neo4j.driver.GraphDatabase.driver(uri, AuthTokens.basic(username, password));
            logger.info("Connected to Neo4j database at {}", uri);
        } catch (Exception e) {
            logger.error("Failed to connect to Neo4j database", e);
            throw new RuntimeException("Neo4j connection failed", e);
        }
    }
    
    @Override
    public void connect() {
        // 默认连接，需要在外部设置连接参数
    }
    
    @Override
    public void close() {
        if (driver != null) {
            driver.close();
            logger.info("Disconnected from Neo4j database");
        }
    }
    
    @Override
    public Entity findEntity(String entityId) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n) WHERE id(n) = $id RETURN n",
                Map.of("id", Long.parseLong(entityId))
            );
            
            if (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Node node = record.get("n").asNode();
                return nodeToEntity(node);
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error finding entity: " + entityId, e);
            return null;
        }
    }
    
    public List<Entity> findEntitiesByName(String name) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n) WHERE n.name CONTAINS $name RETURN n",
                Map.of("name", name)
            );
            
            List<Entity> entities = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Node node = record.get("n").asNode();
                entities.add(nodeToEntity(node));
            }
            
            return entities;
        } catch (Exception e) {
            logger.error("Error finding entities by name: " + name, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Relation> getEntityRelations(String entityId) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n)-[r]-(m) WHERE id(n) = $id RETURN r, m",
                Map.of("id", Long.parseLong(entityId))
            );
            
            List<Relation> relations = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Relationship rel = record.get("r").asRelationship();
                Node targetNode = record.get("m").asNode();
                
                relations.add(relationshipToRelation(rel, targetNode));
            }
            
            return relations;
        } catch (Exception e) {
            logger.error("Error getting entity relations: " + entityId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Entity> searchEntities(String query, int limit) {
        try (Session session = driver.session()) {
            String cypher;
            Map<String, Object> params;
            
            if (query == null || query.trim().isEmpty()) {
                // 如果查询为空，返回所有节点
                cypher = "MATCH (n) RETURN n LIMIT $limit";
                params = Map.of("limit", limit);
            } else {
                // 正常搜索
                cypher = "MATCH (n) WHERE n.name CONTAINS $query OR n.description CONTAINS $query " +
                        "RETURN n LIMIT $limit";
                params = Map.of("query", query, "limit", limit);
            }
            
            Result result = session.run(cypher, params);
            
            List<Entity> entities = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Node node = record.get("n").asNode();
                entities.add(nodeToEntity(node));
            }
            
            return entities;
        } catch (Exception e) {
            logger.error("Error searching entities: " + query, e);
            return new ArrayList<>();
        }
    }
    
    private Entity nodeToEntity(Node node) {
        String id = String.valueOf(node.id());
        String name = node.get("name").asString("");
        String type = node.labels().iterator().hasNext() ? 
                     node.labels().iterator().next() : "Unknown";
        String description = node.get("description").asString("");
        
        Map<String, Object> properties = new HashMap<>();
        node.keys().forEach(key -> properties.put(key, node.get(key).asObject()));
        
        return new Entity(id, name, type, description, properties);
    }
    
    private Relation relationshipToRelation(Relationship rel, Node targetNode) {
        String id = String.valueOf(rel.id());
        String type = rel.type();
        String sourceEntityId = String.valueOf(rel.startNodeId());
        String targetEntityId = String.valueOf(rel.endNodeId());
        
        Map<String, Object> properties = new HashMap<>();
        rel.keys().forEach(key -> properties.put(key, rel.get(key).asObject()));
        
        return new Relation(id, type, sourceEntityId, targetEntityId, properties);
    }
    
    @Override
    public List<Relation> getOutgoingRelations(String entityId) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n)-[r]->(m) WHERE id(n) = $id RETURN r, m",
                Map.of("id", Long.parseLong(entityId))
            );
            
            List<Relation> relations = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Relationship rel = record.get("r").asRelationship();
                Node targetNode = record.get("m").asNode();
                
                relations.add(relationshipToRelation(rel, targetNode));
            }
            
            return relations;
        } catch (Exception e) {
            logger.error("Error getting outgoing relations: " + entityId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Relation> getIncomingRelations(String entityId) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n)<-[r]-(m) WHERE id(n) = $id RETURN r, m",
                Map.of("id", Long.parseLong(entityId))
            );
            
            List<Relation> relations = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Relationship rel = record.get("r").asRelationship();
                Node sourceNode = record.get("m").asNode();
                
                relations.add(relationshipToRelation(rel, sourceNode));
            }
            
            return relations;
        } catch (Exception e) {
            logger.error("Error getting incoming relations: " + entityId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Entity> findRelatedEntities(String entityId, String relationType) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n)-[r:" + relationType + "]-(m) WHERE id(n) = $id RETURN m",
                Map.of("id", Long.parseLong(entityId))
            );
            
            List<Entity> entities = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Node node = record.get("m").asNode();
                entities.add(nodeToEntity(node));
            }
            
            return entities;
        } catch (Exception e) {
            logger.error("Error finding related entities: " + entityId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> executeQuery(String query, Map<String, Object> parameters) {
        try (Session session = driver.session()) {
            Result result = session.run(query, parameters);
            
            List<Map<String, Object>> results = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Map<String, Object> row = new HashMap<>();
                record.keys().forEach(key -> row.put(key, record.get(key).asObject()));
                results.add(row);
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Error executing query: " + query, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void executeBatch(List<String> queries) {
        try (Session session = driver.session()) {
            for (String query : queries) {
                session.run(query);
            }
        } catch (Exception e) {
            logger.error("Error executing batch queries", e);
        }
    }
    
    // === 新增的接口方法实现 ===
    
    @Override
    public List<Entity> searchEntitiesByProperty(String propertyName, String value, int limit) {
        try (Session session = driver.session()) {
            String cypher;
            Map<String, Object> params;
            
            if (value == null || value.trim().isEmpty()) {
                // 如果值为空，返回所有有该属性的节点
                cypher = "MATCH (n) WHERE n." + propertyName + " IS NOT NULL RETURN n LIMIT $limit";
                params = Map.of("limit", limit);
            } else {
                // 正常搜索
                cypher = "MATCH (n) WHERE n." + propertyName + " IS NOT NULL AND " +
                        "toLower(toString(n." + propertyName + ")) CONTAINS toLower($value) " +
                        "RETURN n LIMIT $limit";
                params = Map.of("value", value, "limit", limit);
            }
            
            Result result = session.run(cypher, params);
            
            List<Entity> entities = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Node node = record.get("n").asNode();
                entities.add(nodeToEntity(node));
            }
            
            return entities;
        } catch (Exception e) {
            logger.error("Error searching entities by property: " + propertyName, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getAllNodeTypes() {
        try (Session session = driver.session()) {
            // 尝试多种方法获取节点标签
            List<String> labels = new ArrayList<>();
            
            try {
                // 方法1: 使用 db.labels() 过程
                Result result = session.run("CALL db.labels() YIELD label RETURN label");
                while (result.hasNext()) {
                    org.neo4j.driver.Record record = result.next();
                    labels.add(record.get("label").asString());
                }
                
                if (!labels.isEmpty()) {
                    logger.debug("Found {} node types using db.labels()", labels.size());
                    return labels;
                }
            } catch (Exception e) {
                logger.debug("db.labels() failed, trying alternative method", e);
            }
            
            try {
                // 方法2: 直接查询节点标签
                Result result = session.run("MATCH (n) RETURN DISTINCT labels(n) as labels LIMIT 1000");
                Set<String> uniqueLabels = new HashSet<>();
                
                while (result.hasNext()) {
                    org.neo4j.driver.Record record = result.next();
                    List<Object> nodeLabels = record.get("labels").asList();
                    for (Object label : nodeLabels) {
                        uniqueLabels.add(label.toString());
                    }
                }
                
                labels.addAll(uniqueLabels);
                logger.debug("Found {} node types using direct query", labels.size());
                
            } catch (Exception e) {
                logger.error("Both methods failed to get node types", e);
            }
            
            return labels;
        } catch (Exception e) {
            logger.error("Error getting all node types", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getAllRelationshipTypes() {
        try (Session session = driver.session()) {
            Result result = session.run("CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType");
            
            List<String> types = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                types.add(record.get("relationshipType").asString());
            }
            
            return types;
        } catch (Exception e) {
            logger.error("Error getting all relationship types", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getNodeProperties(String nodeType) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n:" + nodeType + ") WITH keys(n) as props UNWIND props as prop RETURN DISTINCT prop"
            );
            
            List<String> properties = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                properties.add(record.get("prop").asString());
            }
            
            return properties;
        } catch (Exception e) {
            logger.error("Error getting node properties for type: " + nodeType, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getRelationshipProperties(String relationshipType) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH ()-[r:" + relationshipType + "]->() WITH keys(r) as props UNWIND props as prop RETURN DISTINCT prop"
            );
            
            List<String> properties = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                properties.add(record.get("prop").asString());
            }
            
            return properties;
        } catch (Exception e) {
            logger.error("Error getting relationship properties for type: " + relationshipType, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public long getNodeCount(String nodeType) {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n:" + nodeType + ") RETURN count(n) as count");
            
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
            
            return 0;
        } catch (Exception e) {
            logger.error("Error getting node count for type: " + nodeType, e);
            return 0;
        }
    }
    
    @Override
    public long getRelationshipCount(String relationshipType) {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH ()-[r:" + relationshipType + "]->() RETURN count(r) as count");
            
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
            
            return 0;
        } catch (Exception e) {
            logger.error("Error getting relationship count for type: " + relationshipType, e);
            return 0;
        }
    }
    
    @Override
    public long getTotalNodeCount() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (n) RETURN count(n) as count");
            
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
            
            return 0;
        } catch (Exception e) {
            logger.error("Error getting total node count", e);
            return 0;
        }
    }
    
    @Override
    public long getTotalRelationshipCount() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH ()-[r]->() RETURN count(r) as count");
            
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
            
            return 0;
        } catch (Exception e) {
            logger.error("Error getting total relationship count", e);
            return 0;
        }
    }
    
    @Override
    public Map<String, Long> getNodeTypeDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        for (String nodeType : getAllNodeTypes()) {
            distribution.put(nodeType, getNodeCount(nodeType));
        }
        return distribution;
    }
    
    @Override
    public Map<String, Long> getRelationshipTypeDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        for (String relType : getAllRelationshipTypes()) {
            distribution.put(relType, getRelationshipCount(relType));
        }
        return distribution;
    }
    
    @Override
    public List<PropertyInfo> analyzeNodeProperties(String nodeType) {
        List<PropertyInfo> properties = new ArrayList<>();
        
        try (Session session = driver.session()) {
            // 获取节点类型的所有属性及其统计信息
            // 使用简化的查询，不获取类型信息以避免type()函数错误
            Result result = session.run(
                "MATCH (n:" + nodeType + ") " +
                "UNWIND keys(n) as key " +
                "RETURN DISTINCT key, " +
                "       count(*) as frequency " +
                "ORDER BY frequency DESC"
            );
            
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                String propertyName = record.get("key").asString();
                long frequency = record.get("frequency").asLong();
                
                PropertyInfo propInfo = new PropertyInfo(propertyName, frequency);
                
                // 尝试获取属性值的类型信息（使用单独的查询）
                try {
                    analyzePropertyTypes(session, nodeType, propertyName, propInfo);
                } catch (Exception typeError) {
                    logger.debug("Could not analyze types for property {}.{}: {}", 
                               nodeType, propertyName, typeError.getMessage());
                    // 设置默认类型
                    propInfo.addValueType("UNKNOWN", 1L);
                }
                
                properties.add(propInfo);
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing node properties for type: {}", nodeType, e);
            // Fallback: 使用简单方法
            for (String propName : getNodeProperties(nodeType)) {
                PropertyInfo propInfo = new PropertyInfo(propName, 1);
                propInfo.addValueType("UNKNOWN", 1L);
                properties.add(propInfo);
            }
        }
        
        return properties;
    }
    
    /**
     * 分析属性的类型分布
     */
    private void analyzePropertyTypes(Session session, String nodeType, String propertyName, PropertyInfo propInfo) {
        try {
            String escapedProperty = escapePropertyName(propertyName);
            
            // 获取属性值的样本来推断类型
            Result typeResult = session.run(
                "MATCH (n:" + nodeType + ") " +
                "WHERE n." + escapedProperty + " IS NOT NULL " +
                "RETURN DISTINCT n." + escapedProperty + " as value " +
                "LIMIT 5"
            );
            
            Map<String, Long> typeCount = new HashMap<>();
            while (typeResult.hasNext()) {
                org.neo4j.driver.Record record = typeResult.next();
                Object value = record.get("value").asObject();
                String javaType = getJavaTypeName(value);
                typeCount.put(javaType, typeCount.getOrDefault(javaType, 0L) + 1L);
            }
            
            // 设置类型分布
            for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
                propInfo.addValueType(entry.getKey(), entry.getValue());
            }
            
            // 如果没有找到任何类型，设置默认类型
            if (typeCount.isEmpty()) {
                propInfo.addValueType("UNKNOWN", 1L);
            }
            
        } catch (Exception e) {
            logger.debug("Error analyzing property types for {}.{}: {}", nodeType, propertyName, e.getMessage());
            propInfo.addValueType("UNKNOWN", 1L);
        }
    }
    
    /**
     * 获取Java对象的类型名称
     */
    private String getJavaTypeName(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "STRING";
        } else if (value instanceof Long || value instanceof Integer) {
            return "INTEGER";
        } else if (value instanceof Double || value instanceof Float) {
            return "FLOAT";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else if (value instanceof java.util.List) {
            return "LIST";
        } else if (value instanceof java.util.Map) {
            return "MAP";
        } else {
            return value.getClass().getSimpleName().toUpperCase();
        }
    }
    
    @Override
    public List<PropertyInfo> analyzeRelationshipProperties(String relationshipType) {
        List<PropertyInfo> properties = new ArrayList<>();
        
        try (Session session = driver.session()) {
            // 获取关系类型的所有属性及其统计信息
            // 使用简化的查询，不获取类型信息以避免type()函数错误
            Result result = session.run(
                "MATCH ()-[r:" + relationshipType + "]->() " +
                "UNWIND keys(r) as key " +
                "RETURN DISTINCT key, " +
                "       count(*) as frequency " +
                "ORDER BY frequency DESC"
            );
            
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                String propertyName = record.get("key").asString();
                long frequency = record.get("frequency").asLong();
                
                PropertyInfo propInfo = new PropertyInfo(propertyName, frequency);
                
                // 尝试获取关系属性值的类型信息
                try {
                    analyzeRelationshipPropertyTypes(session, relationshipType, propertyName, propInfo);
                } catch (Exception typeError) {
                    logger.debug("Could not analyze types for relationship property {}.{}: {}", 
                               relationshipType, propertyName, typeError.getMessage());
                    // 设置默认类型
                    propInfo.addValueType("UNKNOWN", 1L);
                }
                
                properties.add(propInfo);
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing relationship properties for type: {}", relationshipType, e);
            // Fallback: 使用简单方法
            for (String propName : getRelationshipProperties(relationshipType)) {
                PropertyInfo propInfo = new PropertyInfo(propName, 1);
                propInfo.addValueType("UNKNOWN", 1L);
                properties.add(propInfo);
            }
        }
        
        return properties;
    }
    
    /**
     * 分析关系属性的类型分布
     */
    private void analyzeRelationshipPropertyTypes(Session session, String relationshipType, String propertyName, PropertyInfo propInfo) {
        try {
            String escapedProperty = escapePropertyName(propertyName);
            
            // 获取关系属性值的样本来推断类型
            Result typeResult = session.run(
                "MATCH ()-[r:" + relationshipType + "]->() " +
                "WHERE r." + escapedProperty + " IS NOT NULL " +
                "RETURN DISTINCT r." + escapedProperty + " as value " +
                "LIMIT 5"
            );
            
            Map<String, Long> typeCount = new HashMap<>();
            while (typeResult.hasNext()) {
                org.neo4j.driver.Record record = typeResult.next();
                Object value = record.get("value").asObject();
                String javaType = getJavaTypeName(value);
                typeCount.put(javaType, typeCount.getOrDefault(javaType, 0L) + 1L);
            }
            
            // 设置类型分布
            for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
                propInfo.addValueType(entry.getKey(), entry.getValue());
            }
            
            // 如果没有找到任何类型，设置默认类型
            if (typeCount.isEmpty()) {
                propInfo.addValueType("UNKNOWN", 1L);
            }
            
        } catch (Exception e) {
            logger.debug("Error analyzing relationship property types for {}.{}: {}", relationshipType, propertyName, e.getMessage());
            propInfo.addValueType("UNKNOWN", 1L);
        }
    }
    
    @Override
    public List<String> getSamplePropertyValues(String nodeType, String property, int limit) {
        try (Session session = driver.session()) {
            // 对属性名进行转义，处理特殊字符和数字开头的属性名
            String escapedProperty = escapePropertyName(property);
            
            Result result = session.run(
                "MATCH (n:" + nodeType + ") WHERE n." + escapedProperty + " IS NOT NULL " +
                "RETURN n." + escapedProperty + " as value LIMIT " + limit
            );
            
            List<String> values = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Object value = record.get("value").asObject();
                if (value != null) {
                    values.add(value.toString());
                }
            }
            
            return values;
        } catch (Exception e) {
            logger.error("Error getting sample property values for {}:{}", nodeType, property, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 转义属性名称，处理特殊字符和数字开头的情况
     */
    private String escapePropertyName(String propertyName) {
        // 如果属性名包含特殊字符、空格或以数字开头，需要用反引号包围
        if (propertyName.matches(".*[^a-zA-Z0-9_].*") || 
            propertyName.matches("^[0-9].*") || 
            propertyName.contains(" ")) {
            return "`" + propertyName + "`";
        }
        return propertyName;
    }
    
    @Override
    public List<Entity> findNeighbors(String entityId, int maxDepth) {
        try (Session session = driver.session()) {
            String cypher = String.format("""
                MATCH (source)-[*1..%d]-(neighbor)
                WHERE id(source) = $entityId
                RETURN DISTINCT neighbor
                LIMIT 100
                """, maxDepth);
            
            Result result = session.run(cypher, Map.of("entityId", Long.parseLong(entityId)));
            
            List<Entity> neighbors = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Node node = record.get("neighbor").asNode();
                neighbors.add(nodeToEntity(node));
            }
            
            return neighbors;
        } catch (Exception e) {
            logger.error("Error finding neighbors for entity: {}", entityId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Path> findPaths(String sourceId, String targetId, int maxDepth) {
        try (Session session = driver.session()) {
            String cypher = String.format("""
                MATCH path = (source)-[*1..%d]-(target)
                WHERE id(source) = $sourceId AND id(target) = $targetId
                RETURN path
                ORDER BY length(path)
                LIMIT 10
                """, maxDepth);
            
            Result result = session.run(cypher, 
                Map.of("sourceId", Long.parseLong(sourceId), 
                       "targetId", Long.parseLong(targetId)));
            
            List<Path> paths = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                org.neo4j.driver.types.Path neo4jPath = record.get("path").asPath();
                
                Path customPath = convertNeo4jPath(neo4jPath);
                if (customPath != null) {
                    paths.add(customPath);
                }
            }
            
            return paths;
        } catch (Exception e) {
            logger.error("Error finding paths from {} to {}: {}", sourceId, targetId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private Path convertNeo4jPath(org.neo4j.driver.types.Path neo4jPath) {
        try {
            List<Entity> entities = new ArrayList<>();
            List<Relation> relations = new ArrayList<>();
            
            // 转换节点
            for (org.neo4j.driver.types.Node node : neo4jPath.nodes()) {
                entities.add(nodeToEntity(node));
            }
            
            // 转换关系
            for (org.neo4j.driver.types.Relationship rel : neo4jPath.relationships()) {
                relations.add(relationshipToRelation(rel, null));
            }
            
            return new Path(entities, relations);
        } catch (Exception e) {
            logger.error("Error converting Neo4j path", e);
            return null;
        }
    }
    
    @Override
    public List<Entity> findEntitiesInRadius(String centerId, int radius) {
        try (Session session = driver.session()) {
            String cypher = String.format("""
                MATCH (center)-[*1..%d]-(entity)
                WHERE id(center) = $centerId
                RETURN DISTINCT entity, length(shortestPath((center)-[*]-(entity))) as distance
                ORDER BY distance
                LIMIT 200
                """, radius);
            
            Result result = session.run(cypher, Map.of("centerId", Long.parseLong(centerId)));
            
            List<Entity> entitiesInRadius = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Node node = record.get("entity").asNode();
                Entity entity = nodeToEntity(node);
                
                // 添加距离信息到实体属性中
                int distance = record.get("distance").asInt();
                entity.addProperty("distance_from_center", distance);
                
                entitiesInRadius.add(entity);
            }
            
            return entitiesInRadius;
        } catch (Exception e) {
            logger.error("Error finding entities in radius for center: {}", centerId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "Neo4j";
    }
    
    @Override
    public String getVersion() {
        try (Session session = driver.session()) {
            Result result = session.run("CALL dbms.components() YIELD name, versions RETURN name, versions");
            
            if (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                return record.get("name").asString() + " " + record.get("versions").asList().get(0);
            }
            
            return "Neo4j (version unknown)";
        } catch (Exception e) {
            logger.error("Error getting database version", e);
            return "Neo4j (version unknown)";
        }
    }
    
    @Override
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("type", getDatabaseType());
        info.put("version", getVersion());
        info.put("totalNodes", getTotalNodeCount());
        info.put("totalRelationships", getTotalRelationshipCount());
        info.put("nodeTypes", getAllNodeTypes().size());
        info.put("relationshipTypes", getAllRelationshipTypes().size());
        return info;
    }
}
