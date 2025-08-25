package com.tog.graph.api.dto;

import java.util.Map;

/**
 * 智能体任务执行请求
 */
public class AgentTaskRequest {
    private String task;
    private Map<String, Object> context;
    
    public AgentTaskRequest() {}
    
    public AgentTaskRequest(String task) {
        this.task = task;
    }
    
    public AgentTaskRequest(String task, Map<String, Object> context) {
        this.task = task;
        this.context = context;
    }
    
    // Getters and Setters
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}