package com.tog.graph.demo;

import com.tog.graph.config.GraphConfig;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

/**
 * 数据初始化器 - 向Neo4j数据库添加测试数据
 */
public class DataInitializer {
    
    public static void main(String[] args) {
        GraphConfig config = new GraphConfig();
        
        try (Driver driver = GraphDatabase.driver(config.getUri(), 
                AuthTokens.basic(config.getUsername(), config.getPassword()))) {
            
            try (Session session = driver.session()) {
                System.out.println("🔄 开始初始化测试数据...");
                
                // 清除现有数据
                System.out.println("🗑️ 清除现有数据...");
                session.run("MATCH (n) DETACH DELETE n");
                
                // 创建测试数据
                System.out.println("📝 创建测试数据...");
                
                // 创建人物和理论
                session.run("""
                    CREATE (einstein:Person {name: 'Albert Einstein', profession: 'Physicist', nationality: 'German-American'})
                    CREATE (relativity:Theory {name: 'Theory of Relativity', year: 1905})
                    CREATE (einstein)-[:DEVELOPED {year: 1905}]->(relativity)
                    
                    CREATE (newton:Person {name: 'Isaac Newton', profession: 'Physicist', nationality: 'English'})  
                    CREATE (gravity:Theory {name: 'Law of Universal Gravitation', year: 1687})
                    CREATE (newton)-[:DEVELOPED {year: 1687}]->(gravity)
                    
                    CREATE (curie:Person {name: 'Marie Curie', profession: 'Physicist', nationality: 'Polish-French'})
                    CREATE (radium:Element {name: 'Radium', symbol: 'Ra', atomic_number: 88})
                    CREATE (curie)-[:DISCOVERED {year: 1898}]->(radium)
                    
                    CREATE (darwin:Person {name: 'Charles Darwin', profession: 'Biologist', nationality: 'English'})
                    CREATE (evolution:Theory {name: 'Theory of Evolution', year: 1859})
                    CREATE (darwin)-[:DEVELOPED {year: 1859}]->(evolution)
                    
                    CREATE (tesla:Person {name: 'Nikola Tesla', profession: 'Inventor', nationality: 'Serbian-American'})
                    CREATE (ac:Technology {name: 'Alternating Current', type: 'Electrical System'})
                    CREATE (tesla)-[:INVENTED {year: 1888}]->(ac)
                    """);
                
                // 创建更多关系
                session.run("""
                    MATCH (einstein:Person {name: 'Albert Einstein'})
                    MATCH (newton:Person {name: 'Isaac Newton'})
                    CREATE (einstein)-[:INFLUENCED_BY]->(newton)
                    
                    MATCH (curie:Person {name: 'Marie Curie'})
                    MATCH (einstein:Person {name: 'Albert Einstein'})
                    CREATE (curie)-[:CONTEMPORARY_OF]->(einstein)
                    
                    MATCH (darwin:Person {name: 'Charles Darwin'})
                    MATCH (newton:Person {name: 'Isaac Newton'})
                    CREATE (darwin)-[:INFLUENCED_BY]->(newton)
                    """);
                
                // 验证数据
                System.out.println("✅ 验证创建的数据:");
                var result = session.run("MATCH (n) RETURN n.name as name, labels(n) as type");
                
                while (result.hasNext()) {
                    var record = result.next();
                    System.out.println("  - " + record.get("name").asString() + 
                                     " (" + record.get("type").asList() + ")");
                }
                
                // 统计关系
                var relationResult = session.run("MATCH ()-[r]->() RETURN type(r) as relation_type, count(r) as count");
                System.out.println("\n📊 关系统计:");
                while (relationResult.hasNext()) {
                    var record = relationResult.next();
                    System.out.println("  - " + record.get("relation_type").asString() + 
                                     ": " + record.get("count").asInt());
                }
                
                System.out.println("\n🎉 测试数据初始化完成！");
                System.out.println("现在可以运行 GraphReasoningDemo 进行测试了。");
                
            }
        } catch (Exception e) {
            System.err.println("❌ 数据初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}