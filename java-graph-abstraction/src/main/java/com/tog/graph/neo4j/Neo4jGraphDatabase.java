package com.tog.graph.neo4j;

import com.tog.graph.core.*;
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
            Result result = session.run(
                "MATCH (n) WHERE n.name CONTAINS $query OR n.description CONTAINS $query " +
                "RETURN n LIMIT $limit",
                Map.of("query", query, "limit", limit)
            );
            
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
}