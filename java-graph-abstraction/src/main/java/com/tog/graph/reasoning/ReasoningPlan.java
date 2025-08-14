package com.tog.graph.reasoning;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 推理计划 - 定义推理的步骤和执行策略
 */
public class ReasoningPlan {
    private final String question;
    private final List<ReasoningStep> steps;
    private final ReasoningStrategy strategy;
    
    public ReasoningPlan(String question, List<ReasoningStep> steps, ReasoningStrategy strategy) {
        this.question = question;
        this.steps = steps;
        this.strategy = strategy;
    }
    
    public String getQuestion() { return question; }
    public List<ReasoningStep> getSteps() { return steps; }
    public ReasoningStrategy getStrategy() { return strategy; }
    
    /**
     * 推理策略枚举
     */
    public enum ReasoningStrategy {
        SEQUENTIAL,     // 顺序执行
        PARALLEL,       // 并行执行
        ADAPTIVE        // 自适应执行
    }
    
    /**
     * 推理步骤定义
     */
    public static class ReasoningStep {
        private final String stepId;
        private final StepType type;
        private final String description;
        private final List<String> dependencies; // 依赖的步骤ID
        private CompletableFuture<StepResult> future;
        
        public ReasoningStep(String stepId, StepType type, String description, List<String> dependencies) {
            this.stepId = stepId;
            this.type = type;
            this.description = description;
            this.dependencies = dependencies;
        }
        
        // Getters
        public String getStepId() { return stepId; }
        public StepType getType() { return type; }
        public String getDescription() { return description; }
        public List<String> getDependencies() { return dependencies; }
        public CompletableFuture<StepResult> getFuture() { return future; }
        public void setFuture(CompletableFuture<StepResult> future) { this.future = future; }
        
        public enum StepType {
            ENTITY_IDENTIFICATION,    // 实体识别
            RELATION_EXPLORATION,     // 关系探索
            SIMILARITY_CALCULATION,   // 相似度计算
            EVIDENCE_COLLECTION,      // 证据收集
            ANSWER_GENERATION,        // 答案生成
            VALIDATION               // 结果验证
        }
    }
    
    /**
     * 步骤执行结果
     */
    public static class StepResult {
        private final String stepId;
        private final boolean success;
        private final Object data;
        private final String error;
        private final long executionTime;
        
        public StepResult(String stepId, boolean success, Object data, String error, long executionTime) {
            this.stepId = stepId;
            this.success = success;
            this.data = data;
            this.error = error;
            this.executionTime = executionTime;
        }
        
        // Getters
        public String getStepId() { return stepId; }
        public boolean isSuccess() { return success; }
        public Object getData() { return data; }
        public String getError() { return error; }
        public long getExecutionTime() { return executionTime; }
        
        // 静态工厂方法
        public static StepResult success(String stepId, Object data, long executionTime) {
            return new StepResult(stepId, true, data, null, executionTime);
        }
        
        public static StepResult failure(String stepId, String error, long executionTime) {
            return new StepResult(stepId, false, null, error, executionTime);
        }
    }
}