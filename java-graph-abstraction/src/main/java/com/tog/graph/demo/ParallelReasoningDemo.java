package com.tog.graph.demo;

import com.tog.graph.GraphReasoningSystem;
import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.reasoning.ReasoningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 并行推理演示程序
 * 展示高性能的并行推理和任务调度能力
 */
public class ParallelReasoningDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(ParallelReasoningDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Reasoning System Demo ===");
        
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
            System.out.println("Initializing parallel reasoning system...");
            system = new GraphReasoningSystem(config);
            
            // 创建示例数据
            System.out.println("Creating sample data...");
            createSampleData(system.getGraphDatabase());
            
            // 运行并行推理测试
            runParallelTests(system);
            
            // 运行性能基准测试
            runPerformanceBenchmark(system);
            
            // 交互式并行推理
            runInteractiveParallelDemo(system);
            
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
        
        // 并行推理优化配置
        config.setMaxReasoningDepth(3);
        config.setSearchWidth(6);
        config.setEntitySimilarityThreshold(0.25);
        config.setRelationSimilarityThreshold(0.15);
        
        return config;
    }
    
    private static void createSampleData(GraphDatabase db) {
        try {
            // 清空现有数据
            db.executeQuery("MATCH (n) DETACH DELETE n", new HashMap<>());
            
            // 创建示例数据（简化版）
            String[] queries = {
                "CREATE (einstein:Person {id: 'einstein', name: 'Albert Einstein', type: 'Physicist'})",
                "CREATE (curie:Person {id: 'curie', name: 'Marie Curie', type: 'Physicist'})",
                "CREATE (newton:Person {id: 'newton', name: 'Isaac Newton', type: 'Physicist'})",
                "CREATE (relativity:Theory {id: 'relativity', name: 'Theory of Relativity', field: 'Physics'})",
                "CREATE (radioactivity:Discovery {id: 'radioactivity', name: 'Radioactivity', field: 'Physics'})",
                "CREATE (gravity:Law {id: 'gravity', name: 'Law of Universal Gravitation', field: 'Physics'})",
                "MATCH (einstein:Person {id: 'einstein'}), (relativity:Theory {id: 'relativity'}) CREATE (einstein)-[:DEVELOPED {score: 0.95}]->(relativity)",
                "MATCH (curie:Person {id: 'curie'}), (radioactivity:Discovery {id: 'radioactivity'}) CREATE (curie)-[:DISCOVERED {score: 0.9}]->(radioactivity)",
                "MATCH (newton:Person {id: 'newton'}), (gravity:Law {id: 'gravity'}) CREATE (newton)-[:FORMULATED {score: 0.95}]->(gravity)"
            };
            
            for (String query : queries) {
                db.executeQuery(query, new HashMap<>());
            }
            
            System.out.println("Sample data created successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to create sample data", e);
            System.err.println("Failed to create sample data: " + e.getMessage());
        }
    }
    
    private static void runParallelTests(GraphReasoningSystem system) {
        System.out.println("\n=== Parallel Reasoning Tests ===");
        
        String[] testQuestions = {
            "Who developed the Theory of Relativity?",
            "What did Marie Curie discover?",
            "Who formulated the Law of Universal Gravitation?",
            "What theories are related to physics?"
        };
        
        for (String question : testQuestions) {
            System.out.println("\n" + "-".repeat(50));
            System.out.println("Question: " + question);
            System.out.println("-".repeat(50));
            
            try {
                // 传统异步推理
                long asyncStart = System.currentTimeMillis();
                CompletableFuture<ReasoningResult> asyncFuture = system.getReasoner().reasonAsync(question);
                ReasoningResult asyncResult = asyncFuture.get(30, TimeUnit.SECONDS);
                long asyncTime = System.currentTimeMillis() - asyncStart;
                
                System.out.println("Async Reasoning:");
                System.out.println("  Time: " + asyncTime + "ms");
                System.out.println("  Answer: " + asyncResult.getAnswer());
                
                // 并行推理
                long parallelStart = System.currentTimeMillis();
                CompletableFuture<ReasoningResult> parallelFuture = system.getReasoner().reasonParallel(question);
                ReasoningResult parallelResult = parallelFuture.get(30, TimeUnit.SECONDS);
                long parallelTime = System.currentTimeMillis() - parallelStart;
                
                System.out.println("\nParallel Reasoning:");
                System.out.println("  Time: " + parallelTime + "ms");
                System.out.println("  Answer: " + parallelResult.getAnswer());
                
                // 性能比较
                double speedup = asyncTime > 0 ? (double) asyncTime / parallelTime : 1.0;
                System.out.println("\nPerformance:");
                System.out.println("  Speedup: " + String.format("%.2fx", speedup));
                System.out.println("  Improvement: " + (speedup > 1 ? "Faster" : "Slower"));
                
            } catch (Exception e) {
                System.err.println("Test failed: " + e.getMessage());
                logger.error("Parallel test failed for question: " + question, e);
            }
        }
    }
    
    private static void runPerformanceBenchmark(GraphReasoningSystem system) {
        System.out.println("\n=== Performance Benchmark ===");
        
        List<String> benchmarkQuestions = Arrays.asList(
            "Who developed the Theory of Relativity?",
            "What did Marie Curie discover?",
            "Who formulated the Law of Universal Gravitation?",
            "What theories are related to physics?",
            "Who are the famous physicists?",
            "What discoveries were made in physics?"
        );
        
        // 批量并行推理测试
        System.out.println("Testing batch parallel reasoning...");
        
        long batchStart = System.currentTimeMillis();
        try {
            CompletableFuture<List<ReasoningResult>> batchFuture = 
                system.getReasoner().reasonBatchParallel(benchmarkQuestions);
            
            List<ReasoningResult> batchResults = batchFuture.get(60, TimeUnit.SECONDS);
            long batchTime = System.currentTimeMillis() - batchStart;
            
            System.out.println("Batch Results:");
            System.out.println("  Questions: " + benchmarkQuestions.size());
            System.out.println("  Total Time: " + batchTime + "ms");
            System.out.println("  Average Time: " + (batchTime / benchmarkQuestions.size()) + "ms per question");
            System.out.println("  Throughput: " + String.format("%.2f", (benchmarkQuestions.size() * 1000.0) / batchTime) + " questions/sec");
            
            // 显示部分结果
            System.out.println("\nSample Results:");
            for (int i = 0; i < Math.min(3, batchResults.size()); i++) {
                ReasoningResult result = batchResults.get(i);
                System.out.println("  Q: " + result.getQuestion());
                System.out.println("  A: " + result.getAnswer());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Batch test failed: " + e.getMessage());
            logger.error("Batch performance test failed", e);
        }
        
        // 显示系统统计信息
        System.out.println("System Statistics:");
        Map<String, Object> stats = system.getReasoner().getReasoningStats();
        stats.forEach((key, value) -> 
            System.out.println("  " + key + ": " + value));
    }
    
    private static void runInteractiveParallelDemo(GraphReasoningSystem system) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== Interactive Parallel Reasoning Demo ===");
        System.out.println("Experience high-performance parallel reasoning!");
        System.out.println("\nCommands:");
        System.out.println("  <question> - Ask a question using parallel reasoning");
        System.out.println("  smart <question> - Use smart reasoning (auto-selects best strategy)");
        System.out.println("  batch - Enter batch processing mode");
        System.out.println("  stats - Show system statistics");
        System.out.println("  sessions - Show active sessions");
        System.out.println("  report - Show performance report");
        System.out.println("  quit - Exit");
        System.out.println();
        
        while (true) {
            System.out.print("Parallel> ");
            String input = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(input)) {
                break;
            }
            
            if ("stats".equalsIgnoreCase(input)) {
                showSystemStats(system);
                continue;
            }
            
            if ("sessions".equalsIgnoreCase(input)) {
                showActiveSessions(system);
                continue;
            }
            
            if ("report".equalsIgnoreCase(input)) {
                showPerformanceReport(system);
                continue;
            }
            
            if ("batch".equalsIgnoreCase(input)) {
                runBatchMode(system, scanner);
                continue;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            // 处理问题
            boolean useSmart = input.startsWith("smart ");
            String question = useSmart ? input.substring(6).trim() : input;
            
            if (question.isEmpty()) {
                System.out.println("Please provide a question.");
                continue;
            }
            
            try {
                System.out.println("Processing with " + (useSmart ? "smart" : "parallel") + " reasoning...");
                long start = System.currentTimeMillis();
                
                CompletableFuture<ReasoningResult> future = useSmart ? 
                    system.getReasoner().reasonSmart(question) :
                    system.getReasoner().reasonParallel(question);
                
                ReasoningResult result = future.get(30, TimeUnit.SECONDS);
                long totalTime = System.currentTimeMillis() - start;
                
                System.out.println("\n" + "=".repeat(60));
                System.out.println("PARALLEL REASONING RESULT");
                System.out.println("=".repeat(60));
                System.out.println("Question: " + result.getQuestion());
                System.out.println("Answer: " + result.getAnswer());
                System.out.println("Evidence Count: " + result.getEvidences().size());
                System.out.println("Processing Time: " + totalTime + "ms");
                System.out.println("=".repeat(60) + "\n");
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                logger.error("Interactive parallel reasoning failed", e);
            }
        }
        
        System.out.println("Parallel reasoning demo ended. Goodbye!");
        scanner.close();
    }
    
    private static void showSystemStats(GraphReasoningSystem system) {
        System.out.println("\n=== System Statistics ===");
        Map<String, Object> stats = system.getReasoner().getReasoningStats();
        
        if (stats.isEmpty()) {
            System.out.println("No statistics available yet.");
        } else {
            stats.forEach((key, value) -> 
                System.out.println("  " + key + ": " + value));
        }
        System.out.println();
    }
    
    private static void showActiveSessions(GraphReasoningSystem system) {
        System.out.println("\n=== Active Sessions ===");
        Map<String, Object> sessions = system.getReasoner().getActiveSessionsInfo();
        
        if (sessions.isEmpty()) {
            System.out.println("No active sessions.");
        } else {
            sessions.forEach((sessionId, status) -> 
                System.out.println("  " + sessionId + ": " + status.toString()));
        }
        System.out.println();
    }
    
    private static void showPerformanceReport(GraphReasoningSystem system) {
        System.out.println("\n=== Performance Report ===");
        Map<String, Object> report = system.getReasoner().getPerformanceReport();
        report.forEach((key, value) -> 
            System.out.println("  " + key + ": " + value.toString()));
    }
    
    private static void runBatchMode(GraphReasoningSystem system, Scanner scanner) {
        System.out.println("\n=== Batch Processing Mode ===");
        System.out.println("Enter questions (one per line), empty line to start processing:");
        
        List<String> questions = new ArrayList<>();
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
        
        try {
            CompletableFuture<List<ReasoningResult>> future = 
                system.getReasoner().reasonBatchParallel(questions);
            
            List<ReasoningResult> results = future.get(120, TimeUnit.SECONDS);
            long totalTime = System.currentTimeMillis() - start;
            
            System.out.println("\n=== Batch Results ===");
            System.out.println("Total Time: " + totalTime + "ms");
            System.out.println("Average Time: " + (totalTime / questions.size()) + "ms per question");
            System.out.println("Throughput: " + String.format("%.2f", (questions.size() * 1000.0) / totalTime) + " questions/sec");
            System.out.println();
            
            for (int i = 0; i < results.size(); i++) {
                ReasoningResult result = results.get(i);
                System.out.println((i + 1) + ". Q: " + result.getQuestion());
                System.out.println("   A: " + result.getAnswer());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Batch processing failed: " + e.getMessage());
            logger.error("Batch processing failed", e);
        }
    }
}