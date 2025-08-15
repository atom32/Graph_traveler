package com.tog.graph.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多智能体协调器
 * 负责管理和协调多个智能体的工作
 */
public class MultiAgentCoordinator {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiAgentCoordinator.class);
    
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, List<String>> taskTypeToAgents = new ConcurrentHashMap<>();
    
    /**
     * 注册智能体
     */
    public void registerAgent(Agent agent) {
        agents.put(agent.getId(), agent);
        
        // 建立任务类型到智能体的映射
        for (String taskType : agent.getSupportedTaskTypes()) {
            taskTypeToAgents.computeIfAbsent(taskType, k -> new ArrayList<>()).add(agent.getId());
        }
        
        logger.info("Registered agent: {} - {}", agent.getId(), agent.getName());
    }
    
    /**
     * 初始化所有智能体
     */
    public void initializeAll() {
        logger.info("Initializing {} agents...", agents.size());
        
        for (Agent agent : agents.values()) {
            try {
                agent.initialize();
                logger.info("Agent {} initialized successfully", agent.getId());
            } catch (Exception e) {
                logger.error("Failed to initialize agent: {}", agent.getId(), e);
            }
        }
        
        logger.info("All agents initialization completed");
    }
    
    /**
     * 执行任务 - 自动选择合适的智能体
     */
    public AgentResult executeTask(String taskType, String taskDescription) {
        return executeTask(taskType, taskDescription, new HashMap<>());
    }
    
    /**
     * 执行任务 - 自动选择合适的智能体
     */
    public AgentResult executeTask(String taskType, String taskDescription, Map<String, Object> context) {
        List<String> candidateAgentIds = taskTypeToAgents.get(taskType);
        
        if (candidateAgentIds == null || candidateAgentIds.isEmpty()) {
            return AgentResult.failure("No agent available for task type: " + taskType);
        }
        
        // 选择最合适的智能体
        Agent selectedAgent = selectBestAgent(candidateAgentIds, taskType, taskDescription);
        
        if (selectedAgent == null) {
            return AgentResult.failure("No suitable agent found for task: " + taskDescription);
        }
        
        logger.info("Selected agent {} for task: {}", selectedAgent.getId(), taskType);
        
        return selectedAgent.execute(taskType, taskDescription, context);
    }
    
    /**
     * 并行执行多个任务
     */
    public Map<String, AgentResult> executeTasksParallel(Map<String, TaskRequest> tasks) {
        Map<String, CompletableFuture<AgentResult>> futures = new HashMap<>();
        
        for (Map.Entry<String, TaskRequest> entry : tasks.entrySet()) {
            String taskId = entry.getKey();
            TaskRequest request = entry.getValue();
            
            CompletableFuture<AgentResult> future = CompletableFuture.supplyAsync(() -> 
                executeTask(request.getTaskType(), request.getTaskDescription(), request.getContext())
            );
            
            futures.put(taskId, future);
        }
        
        // 等待所有任务完成
        Map<String, AgentResult> results = new HashMap<>();
        for (Map.Entry<String, CompletableFuture<AgentResult>> entry : futures.entrySet()) {
            try {
                results.put(entry.getKey(), entry.getValue().get());
            } catch (Exception e) {
                logger.error("Task {} execution failed", entry.getKey(), e);
                results.put(entry.getKey(), AgentResult.failure("Execution failed: " + e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * 获取所有智能体的状态
     */
    public Map<String, AgentStatus> getAllAgentStatus() {
        Map<String, AgentStatus> statusMap = new HashMap<>();
        for (Agent agent : agents.values()) {
            statusMap.put(agent.getId(), agent.getStatus());
        }
        return statusMap;
    }
    
    /**
     * 获取支持指定任务类型的智能体列表
     */
    public List<Agent> getAgentsForTaskType(String taskType) {
        List<String> agentIds = taskTypeToAgents.get(taskType);
        if (agentIds == null) {
            return new ArrayList<>();
        }
        
        return agentIds.stream()
                .map(agents::get)
                .filter(Objects::nonNull)
                .toList();
    }
    
    /**
     * 选择最合适的智能体
     */
    private Agent selectBestAgent(List<String> candidateAgentIds, String taskType, String taskDescription) {
        Agent bestAgent = null;
        
        for (String agentId : candidateAgentIds) {
            Agent agent = agents.get(agentId);
            
            if (agent != null && 
                agent.getStatus() == AgentStatus.READY && 
                agent.canHandle(taskType, taskDescription)) {
                
                if (bestAgent == null) {
                    bestAgent = agent;
                } else {
                    // 可以在这里添加更复杂的选择逻辑
                    // 比如负载均衡、性能评分等
                }
            }
        }
        
        return bestAgent;
    }
    
    /**
     * 关闭所有智能体
     */
    public void shutdownAll() {
        logger.info("Shutting down {} agents...", agents.size());
        
        for (Agent agent : agents.values()) {
            try {
                agent.shutdown();
                logger.info("Agent {} shutdown successfully", agent.getId());
            } catch (Exception e) {
                logger.error("Failed to shutdown agent: {}", agent.getId(), e);
            }
        }
        
        agents.clear();
        taskTypeToAgents.clear();
        
        logger.info("All agents shutdown completed");
    }
    
    /**
     * 任务请求封装
     */
    public static class TaskRequest {
        private final String taskType;
        private final String taskDescription;
        private final Map<String, Object> context;
        
        public TaskRequest(String taskType, String taskDescription) {
            this(taskType, taskDescription, new HashMap<>());
        }
        
        public TaskRequest(String taskType, String taskDescription, Map<String, Object> context) {
            this.taskType = taskType;
            this.taskDescription = taskDescription;
            this.context = context;
        }
        
        public String getTaskType() { return taskType; }
        public String getTaskDescription() { return taskDescription; }
        public Map<String, Object> getContext() { return context; }
    }
}