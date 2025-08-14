package com.tog.graph.reasoning;

import com.tog.graph.core.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多跳推理状态管理
 * 追踪推理过程中的状态和统计信息
 */
public class MultiHopState {
    private final String question;
    private final long startTime;
    private final ReasoningConfig config;
    
    // 实体管理
    private final Map<Integer, List<Entity>> entitiesByDepth;
    private final Set<String> exploredEntityIds;
    private final Map<String, Integer> entityExplorationCount;
    
    // 统计信息
    private int currentDepth;
    private int totalExploredEntities;
    private int totalExploredRelations;
    private int totalPathsFound;
    
    // 性能统计
    private final Map<String, Long> timingStats;
    private final Map<String, Integer> countStats;
    
    public MultiHopState(String question, List<Entity> startEntities, ReasoningConfig config) {
        this.question = question;
        this.startTime = System.currentTimeMillis();
        this.config = config;
        
        this.entitiesByDepth = new ConcurrentHashMap<>();
        this.exploredEntityIds = ConcurrentHashMap.newKeySet();
        this.entityExplorationCount = new ConcurrentHashMap<>();
        
        this.currentDepth = 0;
        this.totalExploredEntities = 0;
        this.totalExploredRelations = 0;
        this.totalPathsFound = 0;
        
        this.timingStats = new ConcurrentHashMap<>();
        this.countStats = new ConcurrentHashMap<>();
        
        // 初始化起始实体
        if (startEntities != null && !startEntities.isEmpty()) {
            entitiesByDepth.put(0, new ArrayList<>(startEntities));
        }
    }
    
    /**
     * 获取指定深度的实体
     */
    public List<Entity> getEntitiesAtDepth(int depth) {
        return entitiesByDepth.getOrDefault(depth, new ArrayList<>());
    }
    
    /**
     * 添加实体到指定深度
     */
    public synchronized void addEntityAtDepth(int depth, Entity entity) {
        entitiesByDepth.computeIfAbsent(depth, k -> new ArrayList<>()).add(entity);
    }
    
    /**
     * 标记实体为已探索
     */
    public synchronized void markAsExplored(Entity entity) {
        if (!exploredEntityIds.contains(entity.getId())) {
            exploredEntityIds.add(entity.getId());
            totalExploredEntities++;
        }
        
        // 增加探索计数
        entityExplorationCount.merge(entity.getId(), 1, Integer::sum);
    }
    
    /**
     * 检查实体是否已被探索
     */
    public boolean hasExplored(Entity entity) {
        return exploredEntityIds.contains(entity.getId());
    }
    
    /**
     * 获取实体的探索次数
     */
    public int getExplorationCount(Entity entity) {
        return entityExplorationCount.getOrDefault(entity.getId(), 0);
    }
    
    /**
     * 添加已探索的关系数量
     */
    public synchronized void addExploredRelations(int count) {
        totalExploredRelations += count;
    }
    
    /**
     * 增加找到的路径数量
     */
    public synchronized void addFoundPaths(int count) {
        totalPathsFound += count;
    }
    
    /**
     * 进入下一个深度
     */
    public synchronized void nextDepth() {
        currentDepth++;
    }
    
    /**
     * 记录时间统计
     */
    public void recordTiming(String operation, long duration) {
        timingStats.put(operation, duration);
    }
    
    /**
     * 记录计数统计
     */
    public void recordCount(String metric, int count) {
        countStats.put(metric, count);
    }
    
    /**
     * 获取已探索的实体列表
     */
    public List<Entity> getExploredEntities() {
        // 这里简化处理，实际应该维护实体对象的引用
        return new ArrayList<>();
    }
    
    /**
     * 获取已探索的关系数量
     */
    public int getExploredRelations() {
        return totalExploredRelations;
    }
    
    /**
     * 获取运行时间
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 检查是否应该停止推理
     */
    public boolean shouldStop() {
        // 基于多个条件判断是否应该停止
        return currentDepth >= config.getMaxDepth() ||
               totalExploredEntities >= config.getMaxEntities() ||
               getElapsedTime() > 30000 || // 30秒超时
               (totalPathsFound > 0 && getElapsedTime() > 10000); // 找到路径后10秒超时
    }
    
    /**
     * 获取当前深度的实体数量
     */
    public int getEntitiesCountAtDepth(int depth) {
        return entitiesByDepth.getOrDefault(depth, new ArrayList<>()).size();
    }
    
    /**
     * 获取所有深度的实体总数
     */
    public int getTotalEntitiesAcrossDepths() {
        return entitiesByDepth.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * 获取推理统计信息
     */
    public ReasoningStatistics getStatistics() {
        return new ReasoningStatistics(
            question,
            startTime,
            getElapsedTime(),
            currentDepth,
            totalExploredEntities,
            totalExploredRelations,
            totalPathsFound,
            getTotalEntitiesAcrossDepths(),
            new HashMap<>(timingStats),
            new HashMap<>(countStats)
        );
    }
    
    /**
     * 获取状态摘要
     */
    public String getStateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Multi-hop State Summary:\n"));
        summary.append(String.format("  Question: %s\n", question));
        summary.append(String.format("  Current Depth: %d/%d\n", currentDepth, config.getMaxDepth()));
        summary.append(String.format("  Explored Entities: %d\n", totalExploredEntities));
        summary.append(String.format("  Explored Relations: %d\n", totalExploredRelations));
        summary.append(String.format("  Paths Found: %d\n", totalPathsFound));
        summary.append(String.format("  Elapsed Time: %dms\n", getElapsedTime()));
        
        // 按深度显示实体数量
        summary.append("  Entities by Depth:\n");
        for (int depth = 0; depth <= currentDepth; depth++) {
            int count = getEntitiesCountAtDepth(depth);
            if (count > 0) {
                summary.append(String.format("    Depth %d: %d entities\n", depth, count));
            }
        }
        
        return summary.toString();
    }
    
    // Getters
    public String getQuestion() { return question; }
    public long getStartTime() { return startTime; }
    public ReasoningConfig getConfig() { return config; }
    public int getCurrentDepth() { return currentDepth; }
    public int getExploredEntitiesCount() { return totalExploredEntities; }
    public int getTotalExploredRelations() { return totalExploredRelations; }
    public int getTotalPathsFound() { return totalPathsFound; }
    public Map<Integer, List<Entity>> getEntitiesByDepth() { return entitiesByDepth; }
    public Set<String> getExploredEntityIds() { return exploredEntityIds; }
}