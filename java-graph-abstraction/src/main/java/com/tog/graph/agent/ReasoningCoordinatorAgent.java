package com.tog.graph.agent;

import com.tog.graph.llm.LLMService;
import com.tog.graph.prompt.PromptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 推理协调智能体
 * 负责分析问题、制定计划、协调其他智能体工作、整合结果
 */
public class ReasoningCoordinatorAgent implements Agent {
    
    private static final Logger logger = LoggerFactory.getLogger(ReasoningCoordinatorAgent.class);
    
    private final LLMService llmService;
    private final PromptManager promptManager;
    private final MultiAgentCoordinator coordinator;
    private AgentStatus status = AgentStatus.INITIALIZING;
    
    public ReasoningCoordinatorAgent(LLMService llmService, MultiAgentCoordinator coordinator) {
        this.llmService = llmService;
        this.coordinator = coordinator;
        this.promptManager = PromptManager.getInstance();
    }
    
    @Override
    public String getId() {
        return "reasoning-coordinator-agent";
    }
    
    @Override
    public String getName() {
        return "推理协调智能体";
    }
    
    @Override
    public String getDescription() {
        return "分析复杂问题，制定执行计划，协调多个专业智能体协作，整合结果生成最终答案";
    }
    
    @Override
    public List<String> getSupportedTaskTypes() {
        return Arrays.asList("complex_reasoning", "multi_agent_coordination", "question_analysis");
    }
    
    @Override
    public boolean canHandle(String taskType, String taskDescription) {
        return getSupportedTaskTypes().contains(taskType);
    }
    
    @Override
    public AgentResult execute(String taskType, String taskDescription, Map<String, Object> context) {
        if (status != AgentStatus.READY) {
            return AgentResult.failure("Agent not ready: " + status);
        }
        
        status = AgentStatus.BUSY;
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("ReasoningCoordinatorAgent executing: {}", taskDescription);
            
            switch (taskType) {
                case "complex_reasoning":
                    return performComplexReasoning(taskDescription, context);
                case "question_analysis":
                    return analyzeQuestion(taskDescription, context);
                default:
                    return AgentResult.failure("Unsupported task type: " + taskType);
            }
            
        } catch (Exception e) {
            logger.error("ReasoningCoordinatorAgent execution failed", e);
            return AgentResult.failure("Execution failed: " + e.getMessage());
        } finally {
            status = AgentStatus.READY;
        }
    }
    
    /**
     * 执行复杂推理 - 多智能体协作的核心方法
     */
    private AgentResult performComplexReasoning(String question, Map<String, Object> context) {
        logger.info("🎯 开始复杂推理协调: {}", question);
        
        // 1. 分析问题，制定执行计划
        ExecutionPlan plan = analyzeAndPlan(question);
        logger.info("📋 执行计划制定完成: {} 个步骤", plan.getSteps().size());
        
        // 2. 执行计划中的各个步骤
        Map<String, Object> executionContext = new HashMap<>(context);
        List<String> evidences = new ArrayList<>();
        
        for (ExecutionStep step : plan.getSteps()) {
            logger.info("🔄 执行步骤: {} - {}", step.getAgentType(), step.getDescription());
            
            AgentResult stepResult = coordinator.executeTask(
                step.getTaskType(), 
                step.getDescription(), 
                step.getContext()
            );
            
            if (stepResult.isSuccess()) {
                evidences.add(String.format("[%s] %s", step.getAgentType(), stepResult.getResult()));
                
                // 将结果添加到执行上下文中，供后续步骤使用
                executionContext.put(step.getAgentType() + "_result", stepResult);
                
                logger.info("✅ 步骤完成: {}", step.getDescription());
            } else {
                logger.warn("❌ 步骤失败: {} - {}", step.getDescription(), stepResult.getError());
                evidences.add(String.format("[%s] 执行失败: %s", step.getAgentType(), stepResult.getError()));
            }
        }
        
        // 3. 整合所有结果，生成最终答案
        String finalAnswer = synthesizeFinalAnswer(question, plan, evidences, executionContext);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("execution_plan", plan);
        metadata.put("evidences", evidences);
        metadata.put("steps_executed", plan.getSteps().size());
        metadata.put("execution_time", System.currentTimeMillis() - System.currentTimeMillis());
        
        logger.info("🎉 复杂推理协调完成");
        
        return AgentResult.success(finalAnswer, metadata);
    }
    
    /**
     * 分析问题并制定执行计划
     */
    private ExecutionPlan analyzeAndPlan(String question) {
        String planningPrompt = String.format("""
            你是一个智能推理协调专家。分析以下问题，制定多智能体协作执行计划。
            
            问题: %s
            
            可用的智能体:
            1. EntitySearchAgent - 实体搜索专家
            2. RelationshipAnalysisAgent - 关系分析专家
            
            请分析问题类型，制定执行步骤。返回JSON格式:
            {
                "question_type": "关系查询/实体搜索/知识发现",
                "key_entities": ["实体1", "实体2"],
                "execution_steps": [
                    {
                        "step": 1,
                        "agent": "EntitySearchAgent",
                        "task": "entity_search",
                        "description": "搜索关键实体",
                        "target": "实体名称"
                    }
                ]
            }
            """, question);
        
        try {
            String planResponse = llmService.generate(planningPrompt, 0.1, 512);
            return parseExecutionPlan(planResponse, question);
        } catch (Exception e) {
            logger.error("Failed to generate execution plan", e);
            return createDefaultPlan(question);
        }
    }
    
    /**
     * 解析执行计划
     */
    private ExecutionPlan parseExecutionPlan(String response, String question) {
        ExecutionPlan plan = new ExecutionPlan(question);
        
        // 简化的计划解析 - 实际应该使用JSON解析
        if (question.contains("关系") && (question.contains("和") || question.contains("与"))) {
            // 关系查询计划
            String[] entities = extractEntities(question);
            
            for (String entity : entities) {
                plan.addStep(new ExecutionStep(
                    "EntitySearchAgent", "entity_search", 
                    "搜索实体: " + entity, 
                    Map.of("query", entity, "limit", 5)
                ));
            }
            
            plan.addStep(new ExecutionStep(
                "RelationshipAnalysisAgent", "connection_discovery",
                "分析实体间的连接关系",
                Map.of("depth", 3)
            ));
        } else {
            // 默认搜索计划
            plan.addStep(new ExecutionStep(
                "EntitySearchAgent", "entity_search",
                "搜索相关实体: " + question,
                Map.of("query", question, "limit", 10)
            ));
        }
        
        return plan;
    }
    
    /**
     * 创建默认执行计划
     */
    private ExecutionPlan createDefaultPlan(String question) {
        ExecutionPlan plan = new ExecutionPlan(question);
        
        plan.addStep(new ExecutionStep(
            "EntitySearchAgent", "entity_search",
            "搜索相关实体",
            Map.of("query", question, "limit", 10)
        ));
        
        return plan;
    }
    
    /**
     * 从问题中提取实体
     */
    private String[] extractEntities(String question) {
        // 简化的实体提取
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
    
    /**
     * 整合最终答案
     */
    private String synthesizeFinalAnswer(String question, ExecutionPlan plan, 
                                       List<String> evidences, Map<String, Object> context) {
        
        StringBuilder evidenceText = new StringBuilder();
        for (String evidence : evidences) {
            evidenceText.append(evidence).append("\n");
        }
        
        String synthesisPrompt = String.format("""
            基于多智能体协作的执行结果，为用户问题生成综合性答案。
            
            用户问题: %s
            
            智能体执行结果:
            %s
            
            请提供一个连贯、准确、有洞察力的答案，整合所有智能体的发现。
            """, question, evidenceText.toString());
        
        try {
            return llmService.generate(synthesisPrompt, 0.2, 512);
        } catch (Exception e) {
            logger.error("Failed to synthesize final answer", e);
            return "基于多智能体协作分析，找到了相关信息，但答案生成失败。\n\n执行结果:\n" + evidenceText.toString();
        }
    }
    
    private AgentResult analyzeQuestion(String question, Map<String, Object> context) {
        // 问题分析逻辑
        return AgentResult.success("问题分析完成: " + question);
    }
    
    @Override
    public AgentStatus getStatus() {
        return status;
    }
    
    @Override
    public void initialize() {
        logger.info("Initializing ReasoningCoordinatorAgent...");
        status = AgentStatus.READY;
        logger.info("ReasoningCoordinatorAgent initialized successfully");
    }
    
    @Override
    public void shutdown() {
        logger.info("Shutting down ReasoningCoordinatorAgent...");
        status = AgentStatus.SHUTDOWN;
        logger.info("ReasoningCoordinatorAgent shutdown completed");
    }
    
    /**
     * 执行计划
     */
    public static class ExecutionPlan {
        private final String question;
        private final List<ExecutionStep> steps = new ArrayList<>();
        
        public ExecutionPlan(String question) {
            this.question = question;
        }
        
        public void addStep(ExecutionStep step) {
            steps.add(step);
        }
        
        public String getQuestion() { return question; }
        public List<ExecutionStep> getSteps() { return steps; }
    }
    
    /**
     * 执行步骤
     */
    public static class ExecutionStep {
        private final String agentType;
        private final String taskType;
        private final String description;
        private final Map<String, Object> context;
        
        public ExecutionStep(String agentType, String taskType, String description, Map<String, Object> context) {
            this.agentType = agentType;
            this.taskType = taskType;
            this.description = description;
            this.context = context;
        }
        
        public String getAgentType() { return agentType; }
        public String getTaskType() { return taskType; }
        public String getDescription() { return description; }
        public Map<String, Object> getContext() { return context; }
    }
}