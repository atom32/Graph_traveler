package com.tog.graph.demo;

import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.neo4j.Neo4jGraphDatabase;
import com.tog.graph.reasoning.GraphReasoner;
import com.tog.graph.reasoning.SchemaAwareGraphReasoner;
import com.tog.graph.reasoning.ReasoningConfig;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.SimpleSearchEngine;
import com.tog.graph.search.AdvancedGraphSearchEngine;
import com.tog.graph.search.ScoredEntity;
import com.tog.graph.embedding.EmbeddingService;
import com.tog.graph.embedding.OpenAIEmbeddingService;
import com.tog.graph.llm.LLMService;
import com.tog.graph.llm.OpenAIService;
import com.tog.graph.reasoning.ReasoningResult;

import java.util.Scanner;

/**
 * 图推理演示程序
 */
public class GraphReasoningDemo {
    
    private final GraphConfig config;
    private final GraphDatabase database;
    private final EmbeddingService embeddingService;
    private final LLMService llmService;
    private final SearchEngine searchEngine;
    private final GraphReasoner reasoner;
    private final SchemaAwareGraphReasoner schemaAwareReasoner;
    
    public GraphReasoningDemo() {
        this.config = new GraphConfig();
        this.database = new Neo4jGraphDatabase();
        this.embeddingService = new OpenAIEmbeddingService(
            config.getEmbeddingApiKey(), 
            config.getEmbeddingApiUrl(), 
            config.getEmbeddingModel(), 
            config.getEmbeddingCacheSize()
        );
        this.llmService = new OpenAIService(
            config.getOpenaiApiKey(), 
            config.getOpenaiApiUrl(), 
            config.getOpenaiModel()
        );
        // 使用基于Schema分析的高级搜索引擎
        this.searchEngine = new AdvancedGraphSearchEngine(database, embeddingService);
        
        // 创建推理配置
        ReasoningConfig reasoningConfig = new ReasoningConfig();
        reasoningConfig.setMaxDepth(config.getMaxReasoningDepth());
        reasoningConfig.setWidth(config.getSearchWidth());
        reasoningConfig.setEntityThreshold(config.getEntitySimilarityThreshold());
        reasoningConfig.setRelationThreshold(config.getRelationSimilarityThreshold());
        reasoningConfig.setTemperature(config.getTemperature());
        reasoningConfig.setMaxTokens(config.getMaxTokens());
        
        this.reasoner = new GraphReasoner(database, searchEngine, llmService, reasoningConfig);
        
        // 创建基于Schema的智能推理器
        this.schemaAwareReasoner = new SchemaAwareGraphReasoner(database, searchEngine, llmService, reasoningConfig);
    }
    
    public static void main(String[] args) {
        GraphReasoningDemo demo = new GraphReasoningDemo();
        demo.run();
    }
    
    public void run() {
        System.out.println("🚀 图推理演示程序启动");
        System.out.println("=====================================");
        
        try {
            // 初始化数据库连接
            ((Neo4jGraphDatabase) database).connect(config.getUri(), config.getUsername(), config.getPassword());
            System.out.println("✅ 数据库连接成功");
            
            // 初始化搜索引擎（包括Schema分析）
            System.out.println("🔄 正在分析数据库Schema...");
            searchEngine.initialize();
            System.out.println("✅ Schema分析完成");
            
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                System.out.println("\n请选择操作:");
                System.out.println("1. 实体搜索");
                System.out.println("2. 图推理查询 (标准)");
                System.out.println("3. 智能推理查询 (基于Schema)");
                System.out.println("4. 查看数据库Schema");
                System.out.println("5. 退出");
                System.out.print("请输入选项 (1-5): ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        performEntitySearch(scanner);
                        break;
                    case "2":
                        performReasoningQuery(scanner);
                        break;
                    case "3":
                        performSchemaAwareReasoningQuery(scanner);
                        break;
                    case "4":
                        showSchemaInfo();
                        break;
                    case "5":
                        System.out.println("👋 再见！");
                        return;
                    default:
                        System.out.println("❌ 无效选项，请重新选择");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 程序运行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                database.close();
                System.out.println("✅ 数据库连接已关闭");
            } catch (Exception e) {
                System.err.println("❌ 关闭数据库连接时出错: " + e.getMessage());
            }
        }
    }
    
    private void performEntitySearch(Scanner scanner) {
        System.out.print("请输入搜索关键词: ");
        String query = scanner.nextLine().trim();
        
        if (query.isEmpty()) {
            System.out.println("❌ 搜索关键词不能为空");
            return;
        }
        
        try {
            System.out.println("🔍 搜索实体: " + query);
            
            // 如果使用高级搜索引擎，显示搜索策略信息
            if (searchEngine instanceof AdvancedGraphSearchEngine) {
                System.out.println("🧠 使用基于Schema的智能搜索...");
            }
            
            var entities = searchEngine.searchEntities(query, 5);
            
            if (entities.isEmpty()) {
                System.out.println("❌ 未找到相关实体");
                System.out.println("💡 提示: 请确保数据库中有数据，可以先运行 DataInitializer 初始化测试数据");
                
                // 如果是高级搜索引擎，提供更多调试信息
                if (searchEngine instanceof AdvancedGraphSearchEngine) {
                    System.out.println("🔧 调试信息: 可以选择选项3查看数据库Schema，了解可用的数据结构");
                }
            } else {
                System.out.println("✅ 找到 " + entities.size() + " 个相关实体:");
                for (int i = 0; i < entities.size(); i++) {
                    ScoredEntity scoredEntity = entities.get(i);
                    var entity = scoredEntity.getEntity();
                    System.out.println("  " + (i + 1) + ". " + entity.getName() + 
                                     " (类型: " + entity.getType() + 
                                     ", 相似度: " + String.format("%.3f", scoredEntity.getScore()) + ")");
                }
                
                // 显示搜索引擎类型
                if (searchEngine instanceof AdvancedGraphSearchEngine) {
                    System.out.println("🎯 使用了基于Schema的智能搜索策略");
                } else {
                    System.out.println("⚡ 使用了简单搜索引擎");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void performReasoningQuery(Scanner scanner) {
        System.out.print("请输入推理问题: ");
        String question = scanner.nextLine().trim();
        
        if (question.isEmpty()) {
            System.out.println("❌ 问题不能为空");
            return;
        }
        
        try {
            System.out.println("🤔 开始推理: " + question);
            ReasoningResult result = reasoner.reason(question);
            
            System.out.println("\n📋 推理结果:");
            System.out.println("问题: " + result.getQuestion());
            System.out.println("答案: " + result.getAnswer());
            
            if (!result.getEvidences().isEmpty()) {
                System.out.println("\n🔍 支持证据:");
                for (int i = 0; i < result.getEvidences().size(); i++) {
                    String evidence = result.getEvidences().get(i);
                    System.out.println("  " + (i + 1) + ". " + evidence);
                }
            }
            
            if (!result.getReasoningPath().isEmpty()) {
                System.out.println("\n🛤️ 推理路径:");
                for (int i = 0; i < result.getReasoningPath().size(); i++) {
                    var step = result.getReasoningPath().get(i);
                    System.out.println("  步骤 " + (i + 1) + ": " + step.getDescription());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 推理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void performSchemaAwareReasoningQuery(Scanner scanner) {
        System.out.print("请输入推理问题: ");
        String question = scanner.nextLine().trim();
        
        if (question.isEmpty()) {
            System.out.println("❌ 问题不能为空");
            return;
        }
        
        try {
            System.out.println("🧠 开始基于Schema的智能推理: " + question);
            System.out.println("📊 正在分析问题并制定查询策略...");
            
            ReasoningResult result = schemaAwareReasoner.reason(question);
            
            System.out.println("\n📋 智能推理结果:");
            System.out.println("问题: " + result.getQuestion());
            System.out.println("答案: " + result.getAnswer());
            
            if (!result.getEvidences().isEmpty()) {
                System.out.println("\n🔍 推理证据:");
                for (int i = 0; i < result.getEvidences().size(); i++) {
                    String evidence = result.getEvidences().get(i);
                    System.out.println("  " + (i + 1) + ". " + evidence);
                }
            }
            
            if (!result.getReasoningPath().isEmpty()) {
                System.out.println("\n🛤️ 推理路径:");
                for (int i = 0; i < result.getReasoningPath().size(); i++) {
                    var step = result.getReasoningPath().get(i);
                    System.out.println("  步骤 " + (i + 1) + ": " + step.getDescription());
                }
            }
            
            System.out.println("\n💡 说明: 此查询使用了基于Schema的智能实体抽取和查询规划");
            
        } catch (Exception e) {
            System.err.println("❌ 智能推理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showSchemaInfo() {
        try {
            System.out.println("📊 数据库Schema信息:");
            
            if (searchEngine instanceof AdvancedGraphSearchEngine) {
                AdvancedGraphSearchEngine advancedEngine = (AdvancedGraphSearchEngine) searchEngine;
                var schema = advancedEngine.getSchema();
                
                System.out.println(schema.getSummary());
                
                // 显示索引建议
                if (!schema.getIndexSuggestions().isEmpty()) {
                    System.out.println("\n💡 性能优化建议:");
                    for (String suggestion : schema.getIndexSuggestions()) {
                        System.out.println("  " + suggestion);
                    }
                }
                
            } else {
                System.out.println("当前使用的是简单搜索引擎，无Schema分析功能");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 获取Schema信息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}