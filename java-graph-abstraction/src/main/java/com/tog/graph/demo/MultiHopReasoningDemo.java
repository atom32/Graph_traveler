package com.tog.graph.demo;

import com.tog.graph.GraphReasoningSystem;
import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.reasoning.MultiHopResult;
import com.tog.graph.reasoning.ReasoningPath;
import com.tog.graph.reasoning.ReasoningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多跳推理演示程序
 * 展示复杂的图遍历和路径发现能力
 */
public class MultiHopReasoningDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiHopReasoningDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=== Multi-Hop Reasoning System Demo ===");
        
        // 配置系统
        GraphConfig config = createConfig();
        
        // 检查配置
        if (config.getOpenaiApiKey() == null) {
            System.out.println("Please set OPENAI_API_KEY environment variable");
            return;
        }
        
        GraphReasoningSystem system = null;
        
        try {
            // 初始化系统
            System.out.println("Initializing multi-hop reasoning system...");
            system = new GraphReasoningSystem(config);
            
            // 创建复杂的示例数据
            System.out.println("Creating complex sample data...");
            createComplexSampleData(system.getGraphDatabase());
            
            // 运行多跳推理测试
            runMultiHopTests(system);
            
            // 交互式多跳推理
            runInteractiveMultiHopDemo(system);
            
        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (system != null) {
                system.close();
            }
        }
    }
    
    private static GraphConfig createConfig() {
        GraphConfig config = new GraphConfig();
        
        // 从环境变量获取配置
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey != null) {
            config.setOpenaiApiKey(apiKey);
        }
        
        // Neo4j配置
        config.setUri("bolt://localhost:7687");
        config.setUsername("neo4j");
        config.setPassword("password");
        
        // 多跳推理优化配置
        config.setMaxReasoningDepth(4);
        config.setSearchWidth(4);
        config.setEntitySimilarityThreshold(0.2);
        config.setRelationSimilarityThreshold(0.15);
        
        return config;
    }
    
    private static void createComplexSampleData(GraphDatabase db) {
        try {
            // 清空现有数据
            db.executeQuery("MATCH (n) DETACH DELETE n", new HashMap<>());
            
            // 创建更复杂的知识图谱
            String[] queries = {
                // 科学家
                "CREATE (einstein:Person {id: 'einstein', name: 'Albert Einstein', type: 'Physicist', birth_year: 1879, nationality: 'German'})",
                "CREATE (curie:Person {id: 'curie', name: 'Marie Curie', type: 'Physicist', birth_year: 1867, nationality: 'Polish'})",
                "CREATE (newton:Person {id: 'newton', name: 'Isaac Newton', type: 'Physicist', birth_year: 1643, nationality: 'English'})",
                "CREATE (darwin:Person {id: 'darwin', name: 'Charles Darwin', type: 'Biologist', birth_year: 1809, nationality: 'English'})",
                "CREATE (tesla:Person {id: 'tesla', name: 'Nikola Tesla', type: 'Inventor', birth_year: 1856, nationality: 'Serbian'})",
                
                // 地点
                "CREATE (germany:Location {id: 'germany', name: 'Germany', type: 'Country', continent: 'Europe'})",
                "CREATE (poland:Location {id: 'poland', name: 'Poland', type: 'Country', continent: 'Europe'})",
                "CREATE (england:Location {id: 'england', name: 'England', type: 'Country', continent: 'Europe'})",
                "CREATE (usa:Location {id: 'usa', name: 'United States', type: 'Country', continent: 'North America'})",
                "CREATE (princeton:Location {id: 'princeton', name: 'Princeton University', type: 'University', country: 'USA'})",
                "CREATE (cambridge:Location {id: 'cambridge', name: 'Cambridge University', type: 'University', country: 'England'})",
                
                // 理论和发现
                "CREATE (relativity:Theory {id: 'relativity', name: 'Theory of Relativity', field: 'Physics', year: 1905})",
                "CREATE (radioactivity:Discovery {id: 'radioactivity', name: 'Radioactivity', field: 'Physics', year: 1896})",
                "CREATE (gravity:Law {id: 'gravity', name: 'Law of Universal Gravitation', field: 'Physics', year: 1687})",
                "CREATE (evolution:Theory {id: 'evolution', name: 'Theory of Evolution', field: 'Biology', year: 1859})",
                "CREATE (ac_motor:Invention {id: 'ac_motor', name: 'AC Motor', field: 'Engineering', year: 1888})",
                
                // 奖项和荣誉
                "CREATE (nobel_physics:Award {id: 'nobel_physics', name: 'Nobel Prize in Physics', category: 'Science'})",
                "CREATE (nobel_chemistry:Award {id: 'nobel_chemistry', name: 'Nobel Prize in Chemistry', category: 'Science'})",
                "CREATE (royal_society:Organization {id: 'royal_society', name: 'Royal Society', type: 'Scientific Society'})",
                
                // 期刊和出版物
                "CREATE (principia:Publication {id: 'principia', name: 'Principia Mathematica', type: 'Book', year: 1687})",
                "CREATE (origin:Publication {id: 'origin', name: 'On the Origin of Species', type: 'Book', year: 1859})",
                
                // 复杂的关系网络
                
                // 出生地关系
                "MATCH (einstein:Person {id: 'einstein'}), (germany:Location {id: 'germany'}) CREATE (einstein)-[:BORN_IN {score: 0.95}]->(germany)",
                "MATCH (curie:Person {id: 'curie'}), (poland:Location {id: 'poland'}) CREATE (curie)-[:BORN_IN {score: 0.95}]->(poland)",
                "MATCH (newton:Person {id: 'newton'}), (england:Location {id: 'england'}) CREATE (newton)-[:BORN_IN {score: 0.95}]->(england)",
                "MATCH (darwin:Person {id: 'darwin'}), (england:Location {id: 'england'}) CREATE (darwin)-[:BORN_IN {score: 0.95}]->(england)",
                "MATCH (tesla:Person {id: 'tesla'}), (germany:Location {id: 'germany'}) CREATE (tesla)-[:BORN_IN {score: 0.9}]->(germany)",
                
                // 工作地关系
                "MATCH (einstein:Person {id: 'einstein'}), (princeton:Location {id: 'princeton'}) CREATE (einstein)-[:WORKED_AT {score: 0.9}]->(princeton)",
                "MATCH (newton:Person {id: 'newton'}), (cambridge:Location {id: 'cambridge'}) CREATE (newton)-[:WORKED_AT {score: 0.85}]->(cambridge)",
                "MATCH (darwin:Person {id: 'darwin'}), (cambridge:Location {id: 'cambridge'}) CREATE (darwin)-[:STUDIED_AT {score: 0.8}]->(cambridge)",
                
                // 移居关系
                "MATCH (einstein:Person {id: 'einstein'}), (usa:Location {id: 'usa'}) CREATE (einstein)-[:MOVED_TO {score: 0.8}]->(usa)",
                "MATCH (tesla:Person {id: 'tesla'}), (usa:Location {id: 'usa'}) CREATE (tesla)-[:MOVED_TO {score: 0.85}]->(usa)",
                
                // 理论和发现关系
                "MATCH (einstein:Person {id: 'einstein'}), (relativity:Theory {id: 'relativity'}) CREATE (einstein)-[:DEVELOPED {score: 0.98}]->(relativity)",
                "MATCH (curie:Person {id: 'curie'}), (radioactivity:Discovery {id: 'radioactivity'}) CREATE (curie)-[:DISCOVERED {score: 0.95}]->(radioactivity)",
                "MATCH (newton:Person {id: 'newton'}), (gravity:Law {id: 'gravity'}) CREATE (newton)-[:FORMULATED {score: 0.98}]->(gravity)",
                "MATCH (darwin:Person {id: 'darwin'}), (evolution:Theory {id: 'evolution'}) CREATE (darwin)-[:PROPOSED {score: 0.95}]->(evolution)",
                "MATCH (tesla:Person {id: 'tesla'}), (ac_motor:Invention {id: 'ac_motor'}) CREATE (tesla)-[:INVENTED {score: 0.9}]->(ac_motor)",
                
                // 出版物关系
                "MATCH (newton:Person {id: 'newton'}), (principia:Publication {id: 'principia'}) CREATE (newton)-[:AUTHORED {score: 0.95}]->(principia)",
                "MATCH (darwin:Person {id: 'darwin'}), (origin:Publication {id: 'origin'}) CREATE (darwin)-[:AUTHORED {score: 0.95}]->(origin)",
                "MATCH (gravity:Law {id: 'gravity'}), (principia:Publication {id: 'principia'}) CREATE (gravity)-[:PUBLISHED_IN {score: 0.9}]->(principia)",
                "MATCH (evolution:Theory {id: 'evolution'}), (origin:Publication {id: 'origin'}) CREATE (evolution)-[:PUBLISHED_IN {score: 0.9}]->(origin)",
                
                // 奖项关系
                "MATCH (curie:Person {id: 'curie'}), (nobel_physics:Award {id: 'nobel_physics'}) CREATE (curie)-[:WON {score: 0.95, year: 1903}]->(nobel_physics)",
                "MATCH (curie:Person {id: 'curie'}), (nobel_chemistry:Award {id: 'nobel_chemistry'}) CREATE (curie)-[:WON {score: 0.95, year: 1911}]->(nobel_chemistry)",
                "MATCH (einstein:Person {id: 'einstein'}), (nobel_physics:Award {id: 'nobel_physics'}) CREATE (einstein)-[:WON {score: 0.9, year: 1921}]->(nobel_physics)",
                
                // 组织关系
                "MATCH (newton:Person {id: 'newton'}), (royal_society:Organization {id: 'royal_society'}) CREATE (newton)-[:MEMBER_OF {score: 0.9}]->(royal_society)",
                "MATCH (darwin:Person {id: 'darwin'}), (royal_society:Organization {id: 'royal_society'}) CREATE (darwin)-[:MEMBER_OF {score: 0.85}]->(royal_society)",
                
                // 同事和影响关系
                "MATCH (einstein:Person {id: 'einstein'}), (curie:Person {id: 'curie'}) CREATE (einstein)-[:CONTEMPORARY_OF {score: 0.7}]->(curie)",
                "MATCH (curie:Person {id: 'curie'}), (einstein:Person {id: 'einstein'}) CREATE (curie)-[:CONTEMPORARY_OF {score: 0.7}]->(einstein)",
                "MATCH (newton:Person {id: 'newton'}), (einstein:Person {id: 'einstein'}) CREATE (newton)-[:INFLUENCED {score: 0.8}]->(einstein)",
                "MATCH (darwin:Person {id: 'darwin'}), (newton:Person {id: 'newton'}) CREATE (darwin)-[:INFLUENCED_BY {score: 0.6}]->(newton)",
                
                // 地理关系
                "MATCH (princeton:Location {id: 'princeton'}), (usa:Location {id: 'usa'}) CREATE (princeton)-[:LOCATED_IN {score: 0.95}]->(usa)",
                "MATCH (cambridge:Location {id: 'cambridge'}), (england:Location {id: 'england'}) CREATE (cambridge)-[:LOCATED_IN {score: 0.95}]->(england)",
                
                // 领域关系
                "MATCH (relativity:Theory {id: 'relativity'}), (gravity:Law {id: 'gravity'}) CREATE (relativity)-[:BUILDS_ON {score: 0.7}]->(gravity)",
                "MATCH (radioactivity:Discovery {id: 'radioactivity'}), (nobel_physics:Award {id: 'nobel_physics'}) CREATE (radioactivity)-[:LED_TO {score: 0.8}]->(nobel_physics)"
            };
            
            for (String query : queries) {
                db.executeQuery(query, new HashMap<>());
            }
            
            System.out.println("Complex sample data created successfully!");
            System.out.println("Created entities: Scientists, Locations, Theories, Awards, Publications");
            System.out.println("Created relationships: Birth, Work, Discovery, Influence, Awards, etc.");
            
        } catch (Exception e) {
            logger.error("Failed to create complex sample data", e);
            System.err.println("Failed to create sample data: " + e.getMessage());
        }
    }
    
    private static void runMultiHopTests(GraphReasoningSystem system) {
        System.out.println("\n=== Multi-Hop Reasoning Tests ===");
        
        String[] complexQuestions = {
            "Who won Nobel prizes and worked in the same country as Einstein?",
            "What theories were developed by people who studied at Cambridge?",
            "Which scientists were born in Europe and later moved to America?",
            "What discoveries led to Nobel prizes in Physics?",
            "Who influenced Einstein's work on relativity?"
        };
        
        for (String question : complexQuestions) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Question: " + question);
            System.out.println("=".repeat(60));
            
            try {
                // 传统推理
                long traditionalStart = System.currentTimeMillis();
                ReasoningResult traditionalResult = system.reason(question);
                long traditionalTime = System.currentTimeMillis() - traditionalStart;
                
                System.out.println("\n--- Traditional Reasoning ---");
                System.out.println("Time: " + traditionalTime + "ms");
                System.out.println("Answer: " + traditionalResult.getAnswer());
                System.out.println("Evidence count: " + traditionalResult.getEvidences().size());
                
                // 多跳推理
                long multiHopStart = System.currentTimeMillis();
                CompletableFuture<MultiHopResult> multiHopFuture = system.getReasoner().reasonMultiHop(question)
                    .thenApply(MultiHopResult::new);
                MultiHopResult multiHopResult = multiHopFuture.get(60, TimeUnit.SECONDS);
                long multiHopTime = System.currentTimeMillis() - multiHopStart;
                
                System.out.println("\n--- Multi-Hop Reasoning ---");
                System.out.println("Time: " + multiHopTime + "ms");
                System.out.println("Answer: " + multiHopResult.getBestAnswer());
                System.out.println("Confidence: " + String.format("%.1f%%", multiHopResult.getOverallConfidence() * 100));
                System.out.println("Paths found: " + multiHopResult.getPaths().size());
                System.out.println("Quality rating: " + multiHopResult.getQualityRating());
                
                // 显示最佳路径
                if (!multiHopResult.getPaths().isEmpty()) {
                    ReasoningPath bestPath = multiHopResult.getTopPaths(1).get(0);
                    System.out.println("\nBest reasoning path:");
                    System.out.println("  " + bestPath.getPathDescription());
                    System.out.println("  Score: " + String.format("%.3f", bestPath.getFinalScore()));
                }
                
                // 性能比较
                System.out.println("\n--- Performance Comparison ---");
                System.out.println("Speed improvement: " + 
                                 (traditionalTime > multiHopTime ? 
                                  String.format("%.1fx faster", (double)traditionalTime / multiHopTime) :
                                  String.format("%.1fx slower", (double)multiHopTime / traditionalTime)));
                
                // 显示统计信息
                if (multiHopResult.getStatistics() != null) {
                    System.out.println("\nReasoning Statistics:");
                    multiHopResult.getStatistics().forEach((key, value) -> 
                        System.out.println("  " + key + ": " + value));
                }
                
            } catch (Exception e) {
                System.err.println("Test failed: " + e.getMessage());
                logger.error("Multi-hop test failed for question: " + question, e);
            }
            
            // 短暂暂停
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private static void runInteractiveMultiHopDemo(GraphReasoningSystem system) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== Interactive Multi-Hop Reasoning Demo ===");
        System.out.println("Ask complex questions that require multi-step reasoning!");
        System.out.println("\nSample complex questions:");
        System.out.println("- Who won Nobel prizes and worked in the same country as Einstein?");
        System.out.println("- What theories were developed by people who studied at Cambridge?");
        System.out.println("- Which scientists were born in Europe and later moved to America?");
        System.out.println("- What discoveries led to Nobel prizes in Physics?");
        System.out.println("- Who influenced Einstein's work on relativity?");
        System.out.println("\nType 'quit' to exit, 'stats' to show statistics.\n");
        
        while (true) {
            System.out.print("Complex Question: ");
            String input = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(input)) {
                break;
            }
            
            if ("stats".equalsIgnoreCase(input)) {
                System.out.println("\nReasoning Statistics:");
                System.out.println(system.getReasoner().getReasoningStats());
                continue;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            try {
                System.out.println("\nProcessing multi-hop reasoning...");
                long start = System.currentTimeMillis();
                
                // 执行多跳推理
                CompletableFuture<MultiHopResult> future = system.getReasoner().reasonMultiHop(input)
                    .thenApply(MultiHopResult::new);
                MultiHopResult result = future.get(60, TimeUnit.SECONDS);
                
                long totalTime = System.currentTimeMillis() - start;
                
                // 显示结果
                System.out.println("\n" + "=".repeat(80));
                System.out.println("MULTI-HOP REASONING RESULT");
                System.out.println("=".repeat(80));
                System.out.println(result.getSummary());
                
                if (!result.getPaths().isEmpty()) {
                    System.out.println("\nTop Reasoning Paths:");
                    for (int i = 0; i < Math.min(3, result.getPaths().size()); i++) {
                        ReasoningPath path = result.getTopPaths(3).get(i);
                        System.out.println(String.format("  %d. %s (Score: %.3f)", 
                                         i + 1, path.getPathDescription(), path.getFinalScore()));
                    }
                }
                
                System.out.println("\n" + "=".repeat(80));
                System.out.println("Processing completed in " + totalTime + "ms");
                System.out.println("=".repeat(80) + "\n");
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                logger.error("Interactive multi-hop reasoning failed", e);
            }
        }
        
        System.out.println("Multi-hop reasoning demo ended. Goodbye!");
        scanner.close();
    }
}