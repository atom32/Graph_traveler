package com.tog.graph.agent;

import java.util.Map;
import java.util.List;

/**
 * 自主智能体接口
 * 能够独立处理特定类型的任务
 */
public interface Agent {
    
    /**
     * 智能体的唯一标识
     */
    String getId();
    
    /**
     * 智能体的名称
     */
    String getName();
    
    /**
     * 智能体的描述和能力说明
     */
    String getDescription();
    
    /**
     * 智能体能处理的任务类型
     */
    List<String> getSupportedTaskTypes();
    
    /**
     * 判断是否能处理指定任务
     */
    boolean canHandle(String taskType, String taskDescription);
    
    /**
     * 执行任务
     * @param taskType 任务类型
     * @param taskDescription 任务描述
     * @param context 上下文信息
     * @return 任务执行结果
     */
    AgentResult execute(String taskType, String taskDescription, Map<String, Object> context);
    
    /**
     * 获取智能体状态
     */
    AgentStatus getStatus();
    
    /**
     * 初始化智能体
     */
    void initialize();
    
    /**
     * 关闭智能体
     */
    void shutdown();
}