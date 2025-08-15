package com.tog.graph.demo;

import com.tog.graph.config.GraphConfig;
import com.tog.graph.factory.GraphServiceFactory;
import com.tog.graph.factory.FactoryException;
import com.tog.graph.service.*;
import com.tog.graph.reasoning.ReasoningResult;
import com.tog.graph.search.ScoredEntity;
import com.tog.graph.agent.*;
import com.tog.graph.core.GraphDatabase;

import java.util.Scanner;

/**
 * Graph Traveler 统一演示程序
 * 集成所有功能：实体搜索、推理查询、Schema分析等
 * 支持数据库中立架构
 */
public class GraphTravelerDemo {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GraphTravelerDemo.class);
    
    private final GraphReasoningService reasoningService;
    private final Scanner scanner;
    private final MultiAgentCoordinator agentCoordinator;
    
    public GraphTravelerDemo() throws FactoryException {
        GraphConfig config = new GraphConfig();
        this.reasoningService = GraphServiceFactory.createGraphReasoningService(config);
        this.scanner = new Scanner(System.in);
        this.agentCoordinator = new MultiAgentCoordinator();
        
        // 初始化智能体系统
        initializeAgents();
    }
    
    private void initializeAgents() {
        try {
            // 获取数据库和搜索引擎实例
            GraphDatabase database = reasoningService.getGraphDatabase();
            var searchEngine = reasoningService.getSearchEngine();
            
            // 注册智能体
            agentCoordinator.registerAgent(new EntitySearchAgent(searchEngine));
            agentCoordinator.registerAgent(new RelationshipAnalysisAgent(database));
            
            // 初始化所有智能体
            agentCoordinator.initializeAll();
            
            logger.info("Multi-agent system initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize multi-agent system", e);
        }
    }
    
    public static void main(String[] args) {
        try {
            GraphTravelerDemo demo = new GraphTravelerDemo();
            demo.run();
        } catch (FactoryException e) {
            System.err.println("❌ 服务创建失败: " + e.getMessage());
            logger.error("Service creation failed", e);
            System.exit(1);
        }
    }
    
    public void run() {
        printWelcome();
        
        try {
            initializeService();
            runMainLoop();
        } catch (ServiceException e) {
            System.err.println("❌ 服务运行出错: " + e.getMessage());
            logger.error("Service runtime error", e);
        } finally {
            cleanup();
        }
    }
    
    private void printWelcome() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        🌐 Graph Traveler Demo        ║");
        System.out.println("║     智能图推理与知识发现系统          ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
    }
    
    private void initializeService() throws ServiceException {
        System.out.println("🔄 正在初始化服务...");
        reasoningService.initialize();
        
        ServiceStatus status = reasoningService.getStatus();
        if (status.isFullyReady()) {
            System.out.println("✅ 所有服务初始化完成");
        } else {
            System.out.println("⚠️ 部分服务未就绪，某些功能可能不可用");
            printServiceStatus(status);
        }
        System.out.println();
    }
    
    private void runMainLoop() throws ServiceException {
        while (true) {
            showMainMenu();
            String choice = scanner.nextLine().trim();
            
            try {
                if (!handleMenuChoice(choice)) {
                    break; // 用户选择退出
                }
            } catch (ServiceException e) {
                System.err.println("❌ 操作失败: " + e.getMessage());
                System.out.println("按回车键继续...");
                scanner.nextLine();
            }
        }
    }
    
    private void showMainMenu() {
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│              主菜单                 │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 1. 🔍 实体搜索                      │");
        System.out.println("│ 2. 🤔 标准推理查询                  │");
        System.out.println("│ 3. 🧠 智能推理查询 (基于Schema)     │");
        System.out.println("│ 4. 🤖 多智能体协作查询              │");
        System.out.println("│ 5. 📊 数据库Schema分析              │");
        System.out.println("│ 6. 🔧 系统状态检查                  │");
        System.out.println("│ 7. 💡 使用帮助                      │");
        System.out.println("│ 0. 👋 退出程序                      │");
        System.out.println("└─────────────────────────────────────┘");
        System.out.print("请选择操作 (0-7): ");
    }
    
    private boolean handleMenuChoice(String choice) throws ServiceException {
        switch (choice) {
            case "1":
                performEntitySearch();
                break;
            case "2":
                performStandardReasoning();
                break;
            case "3":
                performSchemaAwareReasoning();
                break;
            case "4":
                performMultiAgentQuery();
                break;
            case "5":
                showSchemaAnalysis();
                break;
            case "6":
                showSystemStatus();
                break;
            case "7":
                showHelp();
                break;
            case "0":
                System.out.println("👋 感谢使用 Graph Traveler！再见！");
                return false;
            default:
                System.out.println("❌ 无效选项，请重新选择");
        }
        
        System.out.println("\n按回车键继续...");
        scanner.nextLine();
        return true;
    }
    
    private void performEntitySearch() throws ServiceException {
        System.out.println("\n🔍 实体搜索");
        System.out.println("─────────────");
        System.out.print("请输入搜索关键词: ");
        String query = scanner.nextLine().trim();
        
        if (query.isEmpty()) {
            System.out.println("❌ 搜索关键词不能为空");
            return;
        }
        
        System.out.println("🔍 正在搜索: " + query);
        EntitySearchResult result = reasoningService.searchEntities(query, 10);
        displaySearchResult(result);
    }
    
    private void performStandardReasoning() throws ServiceException {
        System.out.println("\n🤔 标准推理查询");
        System.out.println("─────────────────");
        System.out.print("请输入推理问题: ");
        String question = scanner.nextLine().trim();
        
        if (question.isEmpty()) {
            System.out.println("❌ 问题不能为空");
            return;
        }
        
        System.out.println("🤔 开始推理分析...");
        ReasoningResult result = reasoningService.performReasoning(question);
        displayReasoningResult(result, "标准推理");
    }
    
    private void performSchemaAwareReasoning() throws ServiceException {
        System.out.println("\n🧠 智能推理查询");
        System.out.println("─────────────────");
        System.out.print("请输入推理问题: ");
        String question = scanner.nextLine().trim();
        
        if (question.isEmpty()) {
            System.out.println("❌ 问题不能为空");
            return;
        }
        
        System.out.println("🧠 启动智能推理引擎...");
        System.out.println("📊 分析问题结构，制定查询策略...");
        ReasoningResult result = reasoningService.performSchemaAwareReasoning(question);
        displayReasoningResult(result, "智能推理");
        System.out.println("\n💡 此查询使用了基于Schema的智能实体抽取和查询规划技术");
    }
    
    private void showSchemaAnalysis() throws ServiceException {
        System.out.println("\n📊 数据库Schema分析");
        System.out.println("─────────────────────");
        
        SchemaInfo schemaInfo = reasoningService.getSchemaInfo();
        System.out.println(schemaInfo.getSummary());
        
        if (schemaInfo.isAvailable() && schemaInfo.getSchema() != null) {
            var schema = schemaInfo.getSchema();
            
            // 显示详细统计
            System.out.println("\n📈 详细统计:");
            System.out.println("  节点类型数: " + schema.getNodeTypes().size());
            System.out.println("  关系类型数: " + schema.getRelationshipTypes().size());
            System.out.println("  总节点数: " + schema.getTotalNodes());
            System.out.println("  总关系数: " + schema.getTotalRelationships());
            
            // 显示优化建议
            if (!schema.getIndexSuggestions().isEmpty()) {
                System.out.println("\n💡 性能优化建议:");
                for (String suggestion : schema.getIndexSuggestions()) {
                    System.out.println("  • " + suggestion);
                }
            }
        }
    }
    
    private void showSystemStatus() {
        System.out.println("\n🔧 系统状态检查");
        System.out.println("─────────────────");
        
        ServiceStatus status = reasoningService.getStatus();
        printServiceStatus(status);
        
        System.out.println("\n🔍 组件详情:");
        System.out.println("  • 数据库连接: " + getStatusIcon(status.isDatabaseConnected()) + 
                          (status.isDatabaseConnected() ? " 已连接" : " 连接失败"));
        System.out.println("  • 搜索引擎: " + getStatusIcon(status.isSearchEngineReady()) + 
                          (status.isSearchEngineReady() ? " 就绪" : " 未就绪"));
        System.out.println("  • 标准推理器: " + getStatusIcon(status.isReasonerReady()) + 
                          (status.isReasonerReady() ? " 就绪" : " 未就绪"));
        System.out.println("  • 智能推理器: " + getStatusIcon(status.isSchemaAwareReasonerReady()) + 
                          (status.isSchemaAwareReasonerReady() ? " 就绪" : " 未就绪"));
    }
    
    private void showHelp() {
        System.out.println("\n💡 使用帮助");
        System.out.println("─────────────");
        System.out.println("🔍 实体搜索:");
        System.out.println("  • 输入关键词搜索相关实体");
        System.out.println("  • 支持模糊匹配和语义搜索");
        System.out.println("  • 示例: \"失眠\", \"中药\", \"方剂\"");
        System.out.println();
        System.out.println("🤔 标准推理:");
        System.out.println("  • 基于图结构的逻辑推理");
        System.out.println("  • 适合简单的问答查询");
        System.out.println("  • 示例: \"什么药物可以治疗失眠？\"");
        System.out.println();
        System.out.println("🧠 智能推理:");
        System.out.println("  • 基于Schema的智能查询规划");
        System.out.println("  • 自动实体抽取和关系推理");
        System.out.println("  • 适合复杂的知识发现任务");
        System.out.println("  • 示例: \"我失眠多梦怎么办？\"");
        System.out.println();
        System.out.println("📊 Schema分析:");
        System.out.println("  • 查看数据库结构信息");
        System.out.println("  • 了解可用的节点和关系类型");
        System.out.println("  • 获取性能优化建议");
        System.out.println();
        System.out.println("💡 提示:");
        System.out.println("  • 确保数据库中有数据才能获得有意义的结果");
        System.out.println("  • 可以运行 DataInitializer 初始化测试数据");
        System.out.println("  • 智能推理比标准推理更适合自然语言问题");
    }
    
    private void displaySearchResult(EntitySearchResult result) {
        if (result.isEmpty()) {
            System.out.println("❌ 未找到相关实体");
            System.out.println("💡 建议:");
            System.out.println("  • 尝试使用不同的关键词");
            System.out.println("  • 确保数据库中有相关数据");
            if (result.isAdvancedSearch()) {
                System.out.println("  • 查看Schema分析了解数据结构");
            }
        } else {
            System.out.println("✅ 找到 " + result.getCount() + " 个相关实体:");
            System.out.println();
            
            for (int i = 0; i < result.getEntities().size(); i++) {
                ScoredEntity scoredEntity = result.getEntities().get(i);
                var entity = scoredEntity.getEntity();
                System.out.printf("  %2d. %-20s (类型: %-10s, 相似度: %.3f)%n", 
                                i + 1, 
                                truncate(entity.getName(), 20), 
                                truncate(entity.getType(), 10), 
                                scoredEntity.getScore());
            }
            
            System.out.println();
            if (result.isAdvancedSearch()) {
                System.out.println("🎯 使用了基于Schema的智能搜索策略");
            } else {
                System.out.println("⚡ 使用了基础搜索引擎");
            }
        }
    }
    
    private void displayReasoningResult(ReasoningResult result, String reasoningType) {
        System.out.println("\n📋 " + reasoningType + "结果:");
        System.out.println("─────────────────────");
        System.out.println("❓ 问题: " + result.getQuestion());
        System.out.println("💡 答案: " + result.getAnswer());
        
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
    }
    
    private void printServiceStatus(ServiceStatus status) {
        String statusIcon = status.isFullyReady() ? "✅" : "⚠️";
        String statusText = status.isFullyReady() ? "系统就绪" : "部分功能不可用";
        System.out.println("🔧 系统状态: " + statusIcon + " " + statusText);
    }
    
    private String getStatusIcon(boolean status) {
        return status ? "✅" : "❌";
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
    
    private void cleanup() {
        try {
            reasoningService.close();
            scanner.close();
            System.out.println("✅ 资源清理完成");
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
}