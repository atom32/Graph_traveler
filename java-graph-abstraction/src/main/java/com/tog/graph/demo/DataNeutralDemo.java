package com.tog.graph.demo;

import com.tog.graph.GraphReasoningSystem;
import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.reasoning.ReasoningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 数据中立的图推理系统演示
 * 支持用户自定义数据源，不依赖特定领域的硬编码数据
 */
public class DataNeutralDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(DataNeutralDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=== Data-Neutral Graph Reasoning System Demo ===");
        
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
            System.out.println("Initializing data-neutral reasoning system...");
            system = new GraphReasoningSystem(config);
            
            // 数据源选择
            chooseDataSource(system);
            
            // 交互式问答
            runInteractiveDemo(system);
            
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
    
    /**
     * 让用户选择数据源
     */
    private static void chooseDataSource(GraphReasoningSystem system) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== Data Source Selection ===");
        System.out.println("Choose your data source:");
        System.out.println("1. Use existing data (if any)");
        System.out.println("2. Load from CSV files");
        System.out.println("3. Load from JSON files");
        System.out.println("4. Create minimal demo data");
        System.out.println("5. Connect to external knowledge base");
        
        System.out.print("Enter your choice (1-5): ");
        String choice = scanner.nextLine().trim();
        
        try {
            switch (choice) {
                case "1":
                    useExistingData(system);
                    break;
                case "2":
                    loadFromCSV(system, scanner);
                    break;
                case "3":
                    loadFromJSON(system, scanner);
                    break;
                case "4":
                    createMinimalDemoData(system);
                    break;
                case "5":
                    connectToExternalKB(system, scanner);
                    break;
                default:
                    System.out.println("Invalid choice, using existing data...");
                    useExistingData(system);
            }
        } catch (Exception e) {
            System.err.println("Failed to load data: " + e.getMessage());
            System.out.println("Falling back to minimal demo data...");
            createMinimalDemoData(system);
        }
    }
    
    /**
     * 使用现有数据
     */
    private static void useExistingData(GraphReasoningSystem system) {
        System.out.println("Using existing data in the database...");
        
        // 检查数据库中是否有数据
        try {
            var result = system.getGraphDatabase().executeQuery(
                "MATCH (n) RETURN count(n) as nodeCount", new HashMap<>());
            
            if (!result.isEmpty()) {
                int nodeCount = ((Number) result.get(0).get("nodeCount")).intValue();
                System.out.println("Found " + nodeCount + " nodes in the database.");
                
                if (nodeCount == 0) {
                    System.out.println("Database is empty, creating minimal demo data...");
                    createMinimalDemoData(system);
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking existing data: " + e.getMessage());
            createMinimalDemoData(system);
        }
    }
    
    /**
     * 从CSV文件加载数据
     */
    private static void loadFromCSV(GraphReasoningSystem system, Scanner scanner) {
        System.out.println("CSV data loading is not implemented in this demo.");
        System.out.println("Expected format:");
        System.out.println("entities.csv: id,name,type,properties...");
        System.out.println("relations.csv: source_id,target_id,relation_type,score,properties...");
        System.out.println("Falling back to minimal demo data...");
        createMinimalDemoData(system);
    }
    
    /**
     * 从JSON文件加载数据
     */
    private static void loadFromJSON(GraphReasoningSystem system, Scanner scanner) {
        System.out.print("Enter JSON file path (or press Enter for default): ");
        String filePath = scanner.nextLine().trim();
        
        if (filePath.isEmpty()) {
            filePath = "data/knowledge_graph.json";
        }
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("File not found: " + filePath);
            System.out.println("Expected JSON format:");
            System.out.println("{\n  \"entities\": [...],\n  \"relations\": [...]\n}");
            System.out.println("Falling back to minimal demo data...");
            createMinimalDemoData(system);
            return;
        }
        
        try {
            String content = Files.readString(path);
            // 这里应该解析JSON并创建图数据
            System.out.println("JSON parsing is not fully implemented in this demo.");
            System.out.println("Falling back to minimal demo data...");
            createMinimalDemoData(system);
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
            createMinimalDemoData(system);
        }
    }
    
    /**
     * 创建最小化的演示数据（领域无关）
     */
    private static void createMinimalDemoData(GraphReasoningSystem system) {
        try {
            GraphDatabase db = system.getGraphDatabase();
            
            // 清空现有数据
            db.executeQuery("MATCH (n) DETACH DELETE n", new HashMap<>());
            
            System.out.println("Creating minimal domain-neutral demo data...");
            
            // 创建通用的实体和关系，避免特定领域偏向
            String[] queries = {
                // 通用实体类型
                "CREATE (entity1:Entity {id: 'e1', name: 'Entity A', type: 'Type1'})",
                "CREATE (entity2:Entity {id: 'e2', name: 'Entity B', type: 'Type1'})",
                "CREATE (entity3:Entity {id: 'e3', name: 'Entity C', type: 'Type2'})",
                "CREATE (entity4:Entity {id: 'e4', name: 'Entity D', type: 'Type2'})",
                "CREATE (entity5:Entity {id: 'e5', name: 'Entity E', type: 'Type3'})",
                
                // 通用关系
                "MATCH (e1:Entity {id: 'e1'}), (e3:Entity {id: 'e3'}) CREATE (e1)-[:RELATED_TO {score: 0.8, type: 'association'}]->(e3)",
                "MATCH (e2:Entity {id: 'e2'}), (e4:Entity {id: 'e4'}) CREATE (e2)-[:CONNECTED_WITH {score: 0.9, type: 'connection'}]->(e4)",
                "MATCH (e3:Entity {id: 'e3'}), (e5:Entity {id: 'e5'}) CREATE (e3)-[:LINKED_TO {score: 0.7, type: 'link'}]->(e5)",
                "MATCH (e1:Entity {id: 'e1'}), (e2:Entity {id: 'e2'}) CREATE (e1)-[:SIMILAR_TO {score: 0.6, type: 'similarity'}]->(e2)",
                "MATCH (e4:Entity {id: 'e4'}), (e5:Entity {id: 'e5'}) CREATE (e4)-[:DEPENDS_ON {score: 0.85, type: 'dependency'}]->(e5)"
            };
            
            for (String query : queries) {
                db.executeQuery(query, new HashMap<>());
            }
            
            System.out.println("Minimal demo data created successfully!");
            System.out.println("You can now ask questions like:");
            System.out.println("- What is related to Entity A?");
            System.out.println("- Which entities are connected?");
            System.out.println("- What depends on Entity E?");
            
        } catch (Exception e) {
            logger.error("Failed to create minimal demo data", e);
            System.err.println("Failed to create demo data: " + e.getMessage());
        }
    }
    
    /**
     * 连接到外部知识库
     */
    private static void connectToExternalKB(GraphReasoningSystem system, Scanner scanner) {
        System.out.println("External knowledge base connection options:");
        System.out.println("1. Wikidata SPARQL endpoint");
        System.out.println("2. DBpedia SPARQL endpoint");
        System.out.println("3. Custom SPARQL endpoint");
        System.out.println("4. REST API endpoint");
        
        System.out.print("Enter your choice (1-4): ");
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                System.out.println("Wikidata integration is not implemented in this demo.");
                break;
            case "2":
                System.out.println("DBpedia integration is not implemented in this demo.");
                break;
            case "3":
                System.out.print("Enter SPARQL endpoint URL: ");
                String sparqlUrl = scanner.nextLine().trim();
                System.out.println("Custom SPARQL integration is not implemented in this demo.");
                break;
            case "4":
                System.out.print("Enter REST API URL: ");
                String apiUrl = scanner.nextLine().trim();
                System.out.println("REST API integration is not implemented in this demo.");
                break;
            default:
                System.out.println("Invalid choice.");
        }
        
        System.out.println("Falling back to minimal demo data...");
        createMinimalDemoData(system);
    }
    
    /**
     * 运行交互式演示
     */
    private static void runInteractiveDemo(GraphReasoningSystem system) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== Data-Neutral Interactive Demo ===");
        System.out.println("Ask questions about your knowledge graph!");
        System.out.println("The system will work with whatever data you've loaded.");
        System.out.println("Type 'help' for suggestions, 'quit' to exit.\n");
        
        while (true) {
            System.out.print("Question: ");
            String question = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(question)) {
                break;
            }
            
            if ("help".equalsIgnoreCase(question)) {
                showHelp(system);
                continue;
            }
            
            if (question.isEmpty()) {
                continue;
            }
            
            try {
                System.out.println("Reasoning...");
                long start = System.currentTimeMillis();
                
                CompletableFuture<ReasoningResult> future = system.reasonAsync(question);
                ReasoningResult result = future.get(30, TimeUnit.SECONDS);
                
                long totalTime = System.currentTimeMillis() - start;
                
                System.out.println("\n" + "=".repeat(50));
                System.out.println("Reasoning completed in " + totalTime + "ms");
                System.out.println("Question: " + result.getQuestion());
                System.out.println("Answer: " + result.getAnswer());
                
                if (!result.getEvidences().isEmpty()) {
                    System.out.println("Evidence:");
                    for (int i = 0; i < result.getEvidences().size(); i++) {
                        System.out.println("  " + (i + 1) + ". " + result.getEvidences().get(i));
                    }
                }
                
                System.out.println("=".repeat(50) + "\n");
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
        System.out.println("Demo ended. Goodbye!");
        scanner.close();
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp(GraphReasoningSystem system) {
        System.out.println("\n=== Help ===");
        
        try {
            // 动态分析当前数据库中的实体类型和关系类型
            var entityTypes = system.getGraphDatabase().executeQuery(
                "MATCH (n) RETURN DISTINCT labels(n) as types, count(n) as count", 
                new HashMap<>());
            
            var relationTypes = system.getGraphDatabase().executeQuery(
                "MATCH ()-[r]->() RETURN DISTINCT type(r) as relType, count(r) as count", 
                new HashMap<>());
            
            System.out.println("Available entity types in your data:");
            for (var row : entityTypes) {
                System.out.println("  - " + row.get("types") + " (" + row.get("count") + " entities)");
            }
            
            System.out.println("\nAvailable relation types:");
            for (var row : relationTypes) {
                System.out.println("  - " + row.get("relType") + " (" + row.get("count") + " relations)");
            }
            
            System.out.println("\nSample question patterns:");
            System.out.println("  - What is [entity_name]?");
            System.out.println("  - How is [entity1] related to [entity2]?");
            System.out.println("  - What [relation_type] [entity_name]?");
            System.out.println("  - Find entities similar to [entity_name]");
            
        } catch (Exception e) {
            System.out.println("Could not analyze your data structure.");
            System.out.println("Try asking general questions about entities and their relationships.");
        }
        
        System.out.println();
    }
}