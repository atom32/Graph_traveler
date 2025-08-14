package com.tog.graph.reasoning.parallel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推理任务抽象基类
 * 定义了所有推理任务的通用接口和属性
 */
public abstract class ReasoningTask<T> {
    
    private String taskId;
    private final ReasoningTaskScheduler.TaskType type;
    private final String description;
    private final int priority;
    private final long timeoutMs;
    
    // 时间戳
    private long submitTime;
    private long startTime;
    private long endTime;
    
    // 任务上下文
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    
    // 任务状态
    private volatile TaskStatus status = TaskStatus.PENDING;
    private volatile String errorMessage;
    
    public ReasoningTask(ReasoningTaskScheduler.TaskType type, String description) {
        this(type, description, 0, 30000); // 默认30秒超时
    }
    
    public ReasoningTask(ReasoningTaskScheduler.TaskType type, String description, 
                        int priority, long timeoutMs) {
        this.type = type;
        this.description = description;
        this.priority = priority;
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * 执行任务的核心逻辑
     */
    public abstract T execute() throws Exception;
    
    /**
     * 任务执行前的准备工作
     */
    protected void beforeExecute() {
        status = TaskStatus.RUNNING;
    }
    
    /**
     * 任务执行后的清理工作
     */
    protected void afterExecute(boolean success) {
        status = success ? TaskStatus.COMPLETED : TaskStatus.FAILED;
    }
    
    /**
     * 获取任务执行时间
     */
    public long getExecutionTime() {
        if (startTime > 0 && endTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }
    
    /**
     * 获取任务等待时间
     */
    public long getWaitTime() {
        if (submitTime > 0 && startTime > 0) {
            return startTime - submitTime;
        }
        return 0;
    }
    
    /**
     * 获取任务总时间
     */
    public long getTotalTime() {
        if (submitTime > 0 && endTime > 0) {
            return endTime - submitTime;
        }
        return 0;
    }
    
    /**
     * 设置上下文参数
     */
    public void setContextParameter(String key, Object value) {
        context.put(key, value);
    }
    
    /**
     * 获取上下文参数
     */
    @SuppressWarnings("unchecked")
    public <V> V getContextParameter(String key) {
        return (V) context.get(key);
    }
    
    /**
     * 获取上下文参数（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <V> V getContextParameter(String key, V defaultValue) {
        return (V) context.getOrDefault(key, defaultValue);
    }
    
    /**
     * 检查任务是否已超时
     */
    public boolean isTimeout() {
        if (timeoutMs <= 0) return false;
        
        long currentTime = System.currentTimeMillis();
        if (startTime > 0) {
            return (currentTime - startTime) > timeoutMs;
        } else if (submitTime > 0) {
            return (currentTime - submitTime) > timeoutMs;
        }
        
        return false;
    }
    
    /**
     * 获取任务摘要
     */
    public String getSummary() {
        return String.format("Task[id=%s, type=%s, status=%s, priority=%d, description=%s]",
                           taskId, type, status, priority, description);
    }
    
    /**
     * 获取详细信息
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append(getSummary()).append("\n");
        info.append(String.format("  Submit Time: %d\n", submitTime));
        info.append(String.format("  Start Time: %d\n", startTime));
        info.append(String.format("  End Time: %d\n", endTime));
        info.append(String.format("  Wait Time: %dms\n", getWaitTime()));
        info.append(String.format("  Execution Time: %dms\n", getExecutionTime()));
        info.append(String.format("  Total Time: %dms\n", getTotalTime()));
        info.append(String.format("  Timeout: %dms\n", timeoutMs));
        
        if (errorMessage != null) {
            info.append(String.format("  Error: %s\n", errorMessage));
        }
        
        if (!context.isEmpty()) {
            info.append("  Context:\n");
            context.forEach((key, value) -> 
                info.append(String.format("    %s: %s\n", key, value)));
        }
        
        return info.toString();
    }
    
    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public ReasoningTaskScheduler.TaskType getType() { return type; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public long getTimeoutMs() { return timeoutMs; }
    
    public long getSubmitTime() { return submitTime; }
    public void setSubmitTime(long submitTime) { this.submitTime = submitTime; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Map<String, Object> getContext() { return new ConcurrentHashMap<>(context); }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,    // 等待执行
        RUNNING,    // 正在执行
        COMPLETED,  // 执行完成
        FAILED,     // 执行失败
        CANCELLED   // 已取消
    }
}