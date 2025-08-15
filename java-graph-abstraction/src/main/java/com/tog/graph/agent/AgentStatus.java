package com.tog.graph.agent;

/**
 * 智能体状态
 */
public enum AgentStatus {
    INITIALIZING("初始化中"),
    READY("就绪"),
    BUSY("执行中"),
    ERROR("错误"),
    SHUTDOWN("已关闭");
    
    private final String description;
    
    AgentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}