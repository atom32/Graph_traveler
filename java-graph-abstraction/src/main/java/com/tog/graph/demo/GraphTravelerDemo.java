package com.tog.graph.demo;

import com.tog.graph.config.GraphConfig;
import com.tog.graph.factory.GraphServiceFactory;
import com.tog.graph.factory.FactoryException;
import com.tog.graph.service.*;
import com.tog.graph.reasoning.ReasoningResult;
import com.tog.graph.search.ScoredEntity;
import com.tog.graph.agent.*;
import com.tog.graph.core.GraphDatabase;

import java.util.*;

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
            
            // 注册专业智能体
            agentCoordinator.registerAgent(new EntitySearchAgent(searchEngine));
            agentCoordinator.registerAgent(new RelationshipAnalysisAgent(database));
            
            // 注册协调智能体 - 需要LLM服务
            // 暂时注释掉，因为需要从reasoningService获取LLM服务
            // agentCoordinator.registerAgent(new ReasoningCoordinatorAgent(llmService, agentCoordinator));
            
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
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  🚀 展示最新AI推理技术               ║");
        System.out.println("║  🧠 Schema感知 + 多智能体协作        ║");
        System.out.println("║  🔍 多跳推理 + 可解释AI              ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
        System.out.println("💡 建议先试试「智能推理查询」体验最新技术！");
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
        System.out.println("│        🌐 Graph Traveler Demo       │");
        System.out.println("│         智能图推理技术展示           │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 🚀 核心功能                         │");
        System.out.println("│ 1. 🧠 智能推理查询 (基于Schema)     │");
        System.out.println("│ 2. 🤖 多智能体协作查询 (NEW!)       │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 🔧 辅助功能                         │");
        System.out.println("│ 3. 🔍 实体搜索                      │");
        System.out.println("│ 4. 📊 Schema分析                    │");
        System.out.println("│ 5. ⚙️  高级选项...                  │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 0. 👋 退出程序                      │");
        System.out.println("└─────────────────────────────────────┘");
        System.out.print("请选择操作 (0-5): ");
    }
    
    private boolean handleMenuChoice(String choice) throws ServiceException {
        switch (choice) {
            case "1":
                performSchemaAwareReasoning();
                break;
            case "2":
                performMultiAgentQuery();
                break;
            case "3":
                performEntitySearch();
                break;
            case "4":
                showSchemaAnalysis();
                break;
            case "5":
                showAdvancedOptions();
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
    
    private void showAdvancedOptions() throws ServiceException {
        System.out.println("\n⚙️ 高级选项");
        System.out.println("─────────────");
        System.out.println("1. 🤔 标准推理查询 (传统方法)");
        System.out.println("2. 🔧 系统状态检查");
        System.out.println("3. 💡 使用帮助");
        System.out.println("4. 🧪 技术细节说明");
        System.out.println("0. 返回主菜单");
        System.out.print("请选择 (0-4): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                performStandardReasoning();
                break;
            case "2":
                showSystemStatus();
                break;
            case "3":
                showHelp();
                break;
            case "4":
                showTechnicalDetails();
                break;
            case "0":
                return;
            default:
                System.out.println("❌ 无效选项");
        }
    }
    
    private void showTechnicalDetails() {
        System.out.println("\n🧪 技术架构说明");
        System.out.println("─────────────────");
        System.out.println("📋 系统架构:");
        System.out.println("  • 数据库中立设计 - 支持Neo4j和RDF");
        System.out.println("  • Schema感知推理 - 基于数据库结构优化查询");
        System.out.println("  • 多智能体协作 - 专业化分工提升效果");
        System.out.println("  • 多跳推理 - 发现深层关系连接");
        System.out.println("  • Prompt工程 - 可配置的LLM交互模板");
        System.out.println();
        System.out.println("🤖 智能体类型:");
        System.out.println("  • EntitySearchAgent - 实体搜索专家");
        System.out.println("  • RelationshipAnalysisAgent - 关系分析专家");
        System.out.println("  • ReasoningCoordinatorAgent - 推理协调器");
        System.out.println();
        System.out.println("🔍 搜索策略:");
        System.out.println("  • 语义搜索 - 基于向量嵌入的相似度");
        System.out.println("  • 多策略搜索 - 精确→模糊→部分匹配");
        System.out.println("  • Schema指导 - 基于数据结构优化搜索");
        System.out.println();
        System.out.println("🧠 推理能力:");
        System.out.println("  • 实体识别与抽取");
        System.out.println("  • 多跳关系推理 (最大深度: 3)");
        System.out.println("  • 间接连接发现");
        System.out.println("  • LLM增强的答案生成");
        System.out.println();
        System.out.println("💡 创新特性:");
        System.out.println("  • 可解释的推理过程");
        System.out.println("  • 智能体工作可视化");
        System.out.println("  • 动态查询规划");
        System.out.println("  • 容错与降级机制");
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
    
    private void performMultiAgentQuery() throws ServiceException {
        System.out.println("\n🤖 多智能体协作查询");
        System.out.println("─────────────────────");
        System.out.println("选择查询类型:");
        System.out.println("1. 实体搜索 (EntitySearchAgent)");
        System.out.println("2. 关系分析 (RelationshipAnalysisAgent)");
        System.out.println("3. 智能协作查询 (多智能体协作)");
        System.out.print("请选择 (1-3): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                performAgentEntitySearch();
                break;
            case "2":
                performAgentRelationshipAnalysis();
                break;
            case "3":
                performCollaborativeQuery();
                break;
            default:
                System.out.println("❌ 无效选项");
        }
    }
    
    private void performAgentEntitySearch() {
        System.out.println("\n🔍 智能体实体搜索");
        System.out.println("─────────────────");
        System.out.print("请输入搜索关键词: ");
        String query = scanner.nextLine().trim();
        
        if (query.isEmpty()) {
            System.out.println("❌ 搜索关键词不能为空");
            return;
        }
        
        System.out.println("🤖 EntitySearchAgent 正在工作...");
        
        Map<String, Object> context = new HashMap<>();
        context.put("limit", 10);
        context.put("threshold", 0.3);
        
        AgentResult result = agentCoordinator.executeTask("entity_search", query, context);
        
        displayAgentResult("EntitySearchAgent", result);
    }
    
    private void performAgentRelationshipAnalysis() {
        System.out.println("\n🕸️ 智能体关系分析");
        System.out.println("─────────────────");
        System.out.print("请输入实体ID或名称: ");
        String entityInput = scanner.nextLine().trim();
        
        if (entityInput.isEmpty()) {
            System.out.println("❌ 实体ID不能为空");
            return;
        }
        
        // 如果输入的不是数字，先搜索实体获取ID
        String entityId = entityInput;
        if (!entityInput.matches("\\d+")) {
            System.out.println("🔍 正在搜索实体: " + entityInput);
            try {
                EntitySearchResult searchResult = reasoningService.searchEntities(entityInput, 1);
                if (!searchResult.isEmpty()) {
                    entityId = searchResult.getEntities().get(0).getEntity().getId();
                    System.out.println("✅ 找到实体: " + searchResult.getEntities().get(0).getEntity().getName() + " (ID: " + entityId + ")");
                } else {
                    System.out.println("❌ 未找到相关实体");
                    return;
                }
            } catch (ServiceException e) {
                System.out.println("❌ 搜索实体失败: " + e.getMessage());
                return;
            }
        }
        
        System.out.println("🤖 RelationshipAnalysisAgent 正在分析...");
        
        Map<String, Object> context = new HashMap<>();
        context.put("entity_id", entityId);
        
        AgentResult result = agentCoordinator.executeTask("relationship_analysis", "分析实体关系", context);
        
        displayAgentResult("RelationshipAnalysisAgent", result);
    }
    
    private void performCollaborativeQuery() {
        System.out.println("\n🤝 智能协作推理");
        System.out.println("─────────────────");
        System.out.print("请输入查询问题: ");
        String question = scanner.nextLine().trim();
        
        if (question.isEmpty()) {
            System.out.println("❌ 查询问题不能为空");
            return;
        }
        
        System.out.println("� 智能协体调器分析问题中...");
        
        // 智能分析问题类型并制定执行策略
        if (question.contains("关系") && (question.contains("和") || question.contains("与"))) {
            performRelationshipQuery(question);
        } else if (question.contains("是什么") || question.contains("介绍")) {
            performEntityQuery(question);
        } else {
            performGeneralQuery(question);
        }
    }
    
    private void performRelationshipQuery(String question) {
        System.out.println("🎯 检测到关系查询，启动专门的关系分析流程...");
        
        // 1. 提取实体
        String[] entities = extractEntitiesFromQuestion(question);
        System.out.println("🔍 识别到实体: " + String.join(", ", entities));
        
        Map<String, List<ScoredEntity>> foundEntities = new HashMap<>();
        
        // 2. 并行搜索所有实体
        Map<String, MultiAgentCoordinator.TaskRequest> searchTasks = new HashMap<>();
        for (int i = 0; i < entities.length; i++) {
            searchTasks.put("entity_" + i, new MultiAgentCoordinator.TaskRequest(
                "entity_search", entities[i], Map.of("limit", 3)
            ));
        }
        
        System.out.println("🤖 EntitySearchAgent 并行搜索实体中...");
        Map<String, AgentResult> searchResults = agentCoordinator.executeTasksParallel(searchTasks);
        
        // 3. 收集搜索结果
        List<String> entityIds = new ArrayList<>();
        for (Map.Entry<String, AgentResult> entry : searchResults.entrySet()) {
            if (entry.getValue().isSuccess() && entry.getValue().getMetadata().containsKey("entities")) {
                @SuppressWarnings("unchecked")
                List<ScoredEntity> entities_list = (List<ScoredEntity>) entry.getValue().getMetadata().get("entities");
                if (!entities_list.isEmpty()) {
                    entityIds.add(entities_list.get(0).getEntity().getId());
                    System.out.println("✅ 找到实体: " + entities_list.get(0).getEntity().getName());
                }
            }
        }
        
        // 4. 分析实体间关系
        if (entityIds.size() >= 1) {  // 降低要求，只要找到1个实体就进行分析
            System.out.println("🕸️ RelationshipAnalysisAgent 分析实体关系中...");
            
            for (String entityId : entityIds) {
                AgentResult relationResult = agentCoordinator.executeTask(
                    "relationship_analysis", "分析实体关系", 
                    Map.of("entity_id", entityId)
                );
                
                if (relationResult.isSuccess()) {
                    System.out.println("📊 关系分析结果:");
                    System.out.println(relationResult.getResult());
                }
            }
            
            // 5. 寻找连接路径
            if (entityIds.size() == 2) {
                AgentResult pathResult = agentCoordinator.executeTask(
                    "path_finding", "寻找连接路径",
                    Map.of("source_id", entityIds.get(0), "target_id", entityIds.get(1), "max_depth", 4)
                );
                
                if (pathResult.isSuccess()) {
                    System.out.println("🛤️ 连接路径分析:");
                    System.out.println(pathResult.getResult());
                }
            }
        }
        
        // 5. 如果没找到足够实体，尝试直接搜索问题
        if (entityIds.size() < 2) {
            System.out.println("⚠️ 只找到部分实体，尝试直接搜索问题...");
            
            AgentResult directResult = agentCoordinator.executeTask(
                "entity_search", question, Map.of("limit", 10)
            );
            
            if (directResult.isSuccess()) {
                System.out.println("🔍 直接搜索结果:");
                System.out.println(directResult.getResult());
                
                // 对找到的实体进行关系分析
                if (directResult.getMetadata().containsKey("entities")) {
                    @SuppressWarnings("unchecked")
                    List<ScoredEntity> directEntities = (List<ScoredEntity>) directResult.getMetadata().get("entities");
                    
                    for (ScoredEntity entity : directEntities.subList(0, Math.min(3, directEntities.size()))) {
                        AgentResult relationResult = agentCoordinator.executeTask(
                            "relationship_analysis", "分析实体关系", 
                            Map.of("entity_id", entity.getEntity().getId())
                        );
                        
                        if (relationResult.isSuccess()) {
                            System.out.println("📊 " + entity.getEntity().getName() + " 的关系分析:");
                            System.out.println(relationResult.getResult());
                        }
                    }
                }
            }
        }
        
        // 6. 综合分析和结论生成
        System.out.println("\n🧠 正在综合分析所有发现...");
        generateCollaborativeConclusion(question);
        
        System.out.println("\n🎉 智能协作推理完成！");
    }
    
    private void performEntityQuery(String question) {
        System.out.println("🎯 检测到实体查询，启动实体分析流程...");
        
        AgentResult searchResult = agentCoordinator.executeTask(
            "entity_search", question, Map.of("limit", 5)
        );
        
        if (searchResult.isSuccess()) {
            System.out.println("🔍 实体搜索结果:");
            System.out.println(searchResult.getResult());
            
            // 对找到的实体进行详细分析
            if (searchResult.getMetadata().containsKey("entities")) {
                @SuppressWarnings("unchecked")
                List<ScoredEntity> entities = (List<ScoredEntity>) searchResult.getMetadata().get("entities");
                
                if (!entities.isEmpty()) {
                    String entityId = entities.get(0).getEntity().getId();
                    
                    AgentResult analysisResult = agentCoordinator.executeTask(
                        "relationship_analysis", "详细分析实体",
                        Map.of("entity_id", entityId)
                    );
                    
                    if (analysisResult.isSuccess()) {
                        System.out.println("📈 实体详细分析:");
                        System.out.println(analysisResult.getResult());
                    }
                }
            }
        }
    }
    
    private void performGeneralQuery(String question) {
        System.out.println("🎯 执行通用智能查询...");
        
        // 先搜索相关实体
        AgentResult searchResult = agentCoordinator.executeTask(
            "entity_search", question, Map.of("limit", 10)
        );
        
        if (searchResult.isSuccess()) {
            System.out.println("🔍 相关实体:");
            System.out.println(searchResult.getResult());
        }
    }
    
    private String[] extractEntitiesFromQuestion(String question) {
        // 简化的实体提取逻辑
        String[] words = question.split("[\\s，。！？、的和与]+");
        List<String> entities = new ArrayList<>();
        
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 4 && 
                word.matches("[\u4e00-\u9fa5]+") && 
                !word.matches(".*[关系什么怎么样哪里为什么].*")) {
                entities.add(word);
            }
        }
        
        return entities.toArray(new String[0]);
    }
    
    private void generateCollaborativeConclusion(String question) {
        System.out.println("💡 智能结论生成:");
        System.out.println("─────────────────");
        
        // 使用LLM生成智能结论 - 但我们没有直接访问LLM服务
        // 作为Demo，我们使用SchemaAwareReasoner来生成结论
        try {
            System.out.println("🤖 启动结论生成智能体...");
            
            // 创建一个简化的问题让Schema推理器生成结论
            String conclusionPrompt = "基于以上多智能体分析结果，请总结：" + question;
            
            ReasoningResult conclusionResult = reasoningService.performSchemaAwareReasoning(conclusionPrompt);
            
            if (conclusionResult != null && conclusionResult.getAnswer() != null) {
                System.out.println("🧠 AI生成的综合结论:");
                System.out.println(conclusionResult.getAnswer());
            } else {
                // 如果LLM失败，提供基于规则的分析
                generateRuleBasedConclusion(question);
            }
            
        } catch (Exception e) {
            logger.debug("Failed to generate AI conclusion, using rule-based approach", e);
            generateRuleBasedConclusion(question);
        }
    }
    
    private void generateRuleBasedConclusion(String question) {
        System.out.println("📋 基于规则的结论分析:");
        
        // 分析问题类型
        if (question.contains("关系") && question.contains("与")) {
            System.out.println("  • 检测到关系查询类型");
            System.out.println("  • 已通过多个专业智能体进行分析");
            System.out.println("  • 建议综合查看上述各智能体的发现");
        } else if (question.contains("是什么") || question.contains("介绍")) {
            System.out.println("  • 检测到实体查询类型");
            System.out.println("  • 已通过实体搜索智能体进行分析");
        } else {
            System.out.println("  • 检测到通用查询类型");
            System.out.println("  • 已通过多智能体协作进行分析");
        }
        
        System.out.println("  • 多智能体系统的优势在于过程透明化");
        System.out.println("  • 每个智能体的专业分析结果如上所示");
    }
    
    private void displayAgentResult(String agentName, AgentResult result) {
        System.out.println(String.format("\n📋 %s 执行结果:", agentName));
        System.out.println("─────────────────────");
        
        if (result.isSuccess()) {
            System.out.println("✅ 执行成功");
            System.out.println(result.getResult());
            
            if (!result.getMetadata().isEmpty()) {
                System.out.println("\n📊 详细信息:");
                for (Map.Entry<String, Object> entry : result.getMetadata().entrySet()) {
                    if (!"entities".equals(entry.getKey()) && !"relations".equals(entry.getKey())) {
                        System.out.println(String.format("  %s: %s", entry.getKey(), entry.getValue()));
                    }
                }
            }
        } else {
            System.out.println("❌ 执行失败: " + result.getError());
        }
        
        if (result.getExecutionTime() > 0) {
            System.out.println(String.format("⏱️ 执行时间: %d ms", result.getExecutionTime()));
        }
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
        System.out.println("\n💡 使用指南");
        System.out.println("─────────────");
        System.out.println("🚀 推荐使用顺序:");
        System.out.println("  1️⃣ 先试试「智能推理查询」- 体验Schema感知技术");
        System.out.println("  2️⃣ 再试试「多智能体协作」- 看看AI如何分工合作");
        System.out.println("  3️⃣ 查看「Schema分析」- 了解数据库结构");
        System.out.println();
        System.out.println("🧠 智能推理查询 (推荐!):");
        System.out.println("  • 🎯 最先进的推理技术");
        System.out.println("  • 🔍 自动实体识别与多跳推理");
        System.out.println("  • 📊 基于数据库Schema优化查询");
        System.out.println("  • 💡 示例问题:");
        System.out.println("    - \"张机与吴普的关系？\"");
        System.out.println("    - \"汤液经法是什么相关的医学典籍？\"");
        System.out.println("    - \"失眠的治疗方法有哪些？\"");
        System.out.println();
        System.out.println("🤖 多智能体协作 (创新!):");
        System.out.println("  • 👥 专业智能体分工协作");
        System.out.println("  • 🔍 EntitySearchAgent: 实体搜索专家");
        System.out.println("  • 🕸️ RelationshipAnalysisAgent: 关系分析专家");
        System.out.println("  • 👁️ 完全透明的工作过程");
        System.out.println("  • 💡 适合复杂关系查询");
        System.out.println();
        System.out.println("🔍 实体搜索:");
        System.out.println("  • 🎯 精准的实体查找");
        System.out.println("  • 🔤 支持模糊匹配和语义搜索");
        System.out.println("  • 💡 示例: \"张仲景\", \"伤寒论\", \"中药\"");
        System.out.println();
        System.out.println("💡 使用技巧:");
        System.out.println("  • 🇨🇳 支持中文自然语言查询");
        System.out.println("  • 🔗 关系查询用\"A与B的关系\"格式效果更好");
        System.out.println("  • 📊 查看Schema分析了解数据结构");
        System.out.println("  • 🚀 智能推理比传统方法更强大");
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
            agentCoordinator.shutdownAll();
            scanner.close();
            System.out.println("✅ 资源清理完成");
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
}