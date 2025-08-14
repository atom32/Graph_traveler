package com.tog.graph.demo;

import com.tog.graph.GraphReasoningSystem;
import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.reasoning.ReasoningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步图推理系统演示程序
 * 展示并行推理能力和性能优势
 */
public class AsyncGraphReasoningDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncGraphReasoningDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=== Async Graph Reasoning System Demo ===");
        
        // 配置系统
        GraphConfig config = createConfig();
        
        // 检查配置
        if (config.getOpenaiApiKey() == null) {
            System.out.println("Please set OPENAI_API_KEY environment variable or modify the config");
            return;
        }
        
        GraphReasoningSystem system = null;
        
        try {
            // 初始化系统
            System.out.println("Initializing async reasoning system...");
            system = new GraphReasoningSystem(config);
            
            // 创建示例数据
            System.out.println("Creating sample data...");
            createSampleData(system.getGraphDatabase());
            
            // 运行性能对比测试
            runPerformanceComparison(system);
            
            // 交互式异步问答
            runAsyncInteractiveDemo(system);
            
        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.err.println("Demo failed: " + e.getMessage());
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
        
        return config;
    }
    
    private static void createSampleData(GraphDatabase db) {
        try {
            // 清空现有数据
            db.executeQuery("MATCH (n) DETACH DELETE n", new HashMap<>());
            
            // 创建更复杂的示例数据
            String[] queries = {
                // 科学家
                "CREATE (einstein:Person {id: 'einstein', name: 'Albert Einstein', type: 'Physicist', birth_year: 1879})",
                "CREATE (curie:Person {id: 'curie', name: 'Marie Curie', type: 'Physicist', birth_year: 1867})",
                "CREATE (newton:Person {id: 'newton', name: 'Isaac Newton', type: 'Physicist', birth_year: 1643})",
                "CREATE (darwin:Person {id: 'darwin', name: 'Charles Darwin', type: 'Biologist', birth_year: 1809})",
                
                // 地点
                "CREATE (germany:Location {id: 'germany', name: 'Germany', type: 'Country'})",
                "CREATE (poland:Location {id: 'poland', name: 'Poland', type: 'Country'})",
                "CREATE (england:Location {id: 'england', name: 'England', type: 'Country'})",
                "CREATE (princeton:Location {id: 'princeton', name: 'Princeton University', type: 'University'})",
                
                // 理论和发现
                "CREATE (relativity:Theory {id: 'relativity', name: 'Theory of Relativity', field: 'Physics'})",
                "CREATE (radioactivity:Discovery {id: 'radioactivity', name: 'Radioactivity', field: 'Physics'})",
                "CREATE (gravity:Law {id: 'gravity', name: 'Law of Universal Gravitation', field: 'Physics'})",
                "CREATE (evolution:Theory {id: 'evolution', name: 'Theory of Evolution', field: 'Biology'})",
                
                // 奖项
                "CREATE (nobel_physics:Award {id: 'nobel_physics', name: 'Nobel Prize in Physics'})",
                "CREATE (nobel_chemistry:Award {id: 'nobel_chemistry', name: 'Nobel Prize in Chemistry'})",
                
                // 关系
                "MATCH (einstein:Person {id: 'einstein'}), (germany:Location {id: 'germany'}) CREATE (einstein)-[:BORN_IN {score: 0.9}]->(germany)",
                "MATCH (curie:Person {id: 'curie'}), (poland:Location {id: 'poland'}) CREATE (curie)-[:BORN_IN {score: 0.9}]->(poland)",
                "MATCH (newton:Person {id: 'newton'}), (england:Location {id: 'england'}) CREATE (newton)-[:BORN_IN {score: 0.9}]->(england)",
                "MATCH (darwin:Person {id: 'darwin'}), (england:Location {id: 'england'}) CREATE (darwin)-[:BORN_IN {score: 0.9}]->(england)",
                
                "MATCH (einstein:Person {id: 'einstein'}), (relativity:Theory {id: 'relativity'}) CREATE (einstein)-[:DEVELOPED {score: 0.95}]->(relativity)",
                "MATCH (curie:Person {id: 'curie'}), (radioactivity:Discovery {id: 'radioactivity'}) CREATE (curie)-[:DISCOVERED {score: 0.9}]->(radioactivity)",
                "MATCH (newton:Person {id: 'newton'}), (gravity:Law {id: 'gravity'}) CREATE (newton)-[:FORMULATED {score: 0.95}]->(gravity)",
                "MATCH (darwin:Person {id: 'darwin'}), (evolution:Theory {id: 'evolution'}) CREATE (darwin)-[:PROPOSED {score: 0.9}]->(evolution)",
                
                "MATCH (einstein:Person {id: 'einstein'}), (princeton:Location {id: 'princeton'}) CREATE (einstein)-[:WORKED_AT {score: 0.8}]->(princeton)",
                "MATCH (curie:Person {id: 'curie'}), (nobel_physics:Award {id: 'nobel_physics'}) CREATE (curie)-[:WON {score: 0.9}]->(nobel_physics)",
                "MATCH (curie:Person {id: 'curie'}), (nobel_chemistry:Award {id: 'nobel_chemistry'}) CREATE (curie)-[:WON {score: 0.9}]->(nobel_chemistry)",
                
                "MATCH (einstein:Person {id: 'einstein'}), (curie:Person {id: 'curie'}) CREATE (einstein)-[:CONTEMPORARY_OF {score: 0.7}]->(curie)",
                "MATCH (curie:Person {id: 'curie'}), (einstein:Person {id: 'einstein'}) CREATE (curie)-[:CONTEMPORARY_OF {score: 0.7}]->(einstein)"
            };
            
            for (String query : queries) {
                db.executeQuery(query, new HashMap<>());
            }
            
            System.out.println("Enhanced sample data created successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to create sample data", e);
            System.err.println("Failed to create sample data: " + e.getMessage());
        }
    }
    
    private static void runPerformanceComparison(GraphReasoningSystem system) {
        System.out.println("\n=== Performance Comparison ===");
        
        String[] testQuestions = {
            "Who developed the Theory of Relativity?",
            "Where was Marie Curie born?",
            "What awards did Marie Curie win?",
            "Who worked at Princeton University?"
        };
        
        System.out.println("Testing synchronous vs asynchronous reasoning...\n");
        
        for (String question : testQuestions) {
            System.out.println("Question: " + question);
            
            // 同步推理
            long syncStart = System.currentTimeMillis();
            try {
                ReasoningResult syncResult = system.getReasoner().reasonSync(question);
                long syncTime = System.currentTimeMillis() - syncStart;
                System.out.println("  Sync time: " + syncTime + "ms");
                System.out.println("  Sync answer: " + syncResult.getAnswer());
            } catch (Exception e) {
                System.out.println("  Sync failed: " + e.getMessage());
            }
            
            // 异步推理
            long asyncStart = System.currentTimeMillis();
            try {
                CompletableFuture<ReasoningResult> asyncFuture = system.getReasoner().reasonAsync(question);
                ReasoningResult asyncResult = asyncFuture.get(30, TimeUnit.SECONDS);
                long asyncTime = System.currentTimeMillis() - asyncStart;
                System.out.println("  Async time: " + asyncTime + "ms");
                System.out.println("  Async answer: " + asyncResult.getAnswer());
            } catch (Exception e) {
                System.out.println("  Async failed: " + e.getMessage());
            }
            
            System.out.println();
        }
        
        // 并发测试
        System.out.println("Testing concurrent reasoning...");
        long concurrentStart = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = new CompletableFuture[testQuestions.length];
        for (int i = 0; i < testQuestions.length; i++) {
            final String question = testQuestions[i];
            futures[i] = system.getReasoner().reasonAsync(question)
                .thenAccept(result -> {
                    System.out.println("  Concurrent result for '" + question + "': " + result.getAnswer());
                });
        }
        
        try {
            CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);
            long concurrentTime = System.currentTimeMillis() - concurrentStart;
            System.out.println("Total concurrent time: " + concurrentTime + "ms");
        } catch (Exception e) {
            System.out.println("Concurrent test failed: " + e.getMessage());
        }
    }
    
    private static void runAsyncInteractiveDemo(GraphReasoningSystem system) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== Async Interactive Demo ===");
        System.out.println("Ask questions and see async reasoning in action!");
        System.out.println("Sample questions:");
        System.out.println("- Who developed the Theory of Relativity?");
        System.out.println("- Where was Marie Curie born?");
        System.out.println("- What did Charles Darwin propose?");
        System.out.println("- Who won Nobel prizes?");
        System.out.println("Type 'quit' to exit, 'batch' for batch processing.\n");
        
        while (true) {
            System.out.print("Question: ");
            String input = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(input)) {
                break;
            }
            
            if ("batch".equalsIgnoreCase(input)) {
                runBatchProcessing(system, scanner);
                continue;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            try {
                System.out.println("Reasoning asynchronously...");
                long start = System.currentTimeMillis();
                
                CompletableFuture<ReasoningResult> future = system.getReasoner().reasonAsync(input);
                
                // 显示进度
                showProgress(future);
                
                ReasoningResult result = future.get(30, TimeUnit.SECONDS);
                long totalTime = System.currentTimeMillis() - start;
                
                System.out.println("\n" + "=".repeat(50));
                System.out.println("Reasoning completed in " + totalTime + "ms");
                System.out.println(result);
                System.out.println("=".repeat(50) + "\n");
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
        System.out.println("Demo ended. Goodbye!");
        scanner.close();
    }
    
    private static void runBatchProcessing(GraphReasoningSystem system, Scanner scanner) {
        System.out.println("Enter questions (one per line), empty line to start batch processing:");
        
        java.util.List<String> questions = new java.util.ArrayList<>();
        String line;
        while (!(line = scanner.nextLine().trim()).isEmpty()) {
            questions.add(line);
        }
        
        if (questions.isEmpty()) {
            System.out.println("No questions provided.");
            return;
        }
        
        System.out.println("Processing " + questions.size() + " questions in parallel...");
        long start = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = questions.stream()
            .map(question -> system.getReasoner().reasonAsync(question)
                .thenAccept(result -> {
                    System.out.println("\nQ: " + question);
                    System.out.println("A: " + result.getAnswer());
                }))
            .toArray(CompletableFuture[]::new);
        
        try {
            CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);
            long totalTime = System.currentTimeMillis() - start;
            System.out.println("\nBatch processing completed in " + totalTime + "ms");
        } catch (Exception e) {
            System.err.println("Batch processing failed: " + e.getMessage());
        }
    }
    
    private static void showProgress(CompletableFuture<ReasoningResult> future) {
        // 简单的进度显示
        new Thread(() -> {
            try {
                while (!future.isDone()) {
                    System.out.print(".");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}