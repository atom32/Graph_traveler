package com.tog.graph.reasoning;

/**
 * 推理配置类
 * 支持多跳推理和高级配置选项
 */
public class ReasoningConfig {
    // 基础推理参数
    private int maxDepth = 3;
    private int width = 3;
    private double entityThreshold = 0.5;
    private double relationThreshold = 0.2;
    
    // LLM配置
    private double temperature = 0.0;
    private int maxTokens = 256;
    private int maxEvidences = 10;
    
    // 多跳推理配置
    private int maxEntities = 100;           // 最大探索实体数
    private int maxPaths = 50;               // 最大路径数
    private long maxReasoningTime = 30000;   // 最大推理时间(ms)
    private double minPathScore = 0.1;       // 最小路径分数
    private boolean enablePathMerging = true; // 启用路径合并
    private boolean enablePathPruning = true; // 启用路径剪枝
    
    // 性能优化配置
    private int threadPoolSize = 4;          // 线程池大小
    private int batchSize = 10;              // 批处理大小
    private boolean enableCaching = true;    // 启用缓存
    private int cacheSize = 1000;            // 缓存大小
    
    // 质量控制配置
    private double confidenceThreshold = 0.3; // 置信度阈值
    private int maxRetries = 3;               // 最大重试次数
    private boolean strictValidation = false; // 严格验证模式
    
    public ReasoningConfig() {}
    
    public ReasoningConfig(int maxDepth, int width, double entityThreshold, double relationThreshold) {
        this.maxDepth = maxDepth;
        this.width = width;
        this.entityThreshold = entityThreshold;
        this.relationThreshold = relationThreshold;
    }
    
    // Getters and Setters
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public double getEntityThreshold() { return entityThreshold; }
    public void setEntityThreshold(double entityThreshold) { this.entityThreshold = entityThreshold; }
    
    public double getRelationThreshold() { return relationThreshold; }
    public void setRelationThreshold(double relationThreshold) { this.relationThreshold = relationThreshold; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    
    public int getMaxEvidences() { return maxEvidences; }
    public void setMaxEvidences(int maxEvidences) { this.maxEvidences = maxEvidences; }
    
    // 多跳推理配置的getter和setter
    public int getMaxEntities() { return maxEntities; }
    public void setMaxEntities(int maxEntities) { this.maxEntities = maxEntities; }
    
    public int getMaxPaths() { return maxPaths; }
    public void setMaxPaths(int maxPaths) { this.maxPaths = maxPaths; }
    
    public long getMaxReasoningTime() { return maxReasoningTime; }
    public void setMaxReasoningTime(long maxReasoningTime) { this.maxReasoningTime = maxReasoningTime; }
    
    public double getMinPathScore() { return minPathScore; }
    public void setMinPathScore(double minPathScore) { this.minPathScore = minPathScore; }
    
    public boolean isEnablePathMerging() { return enablePathMerging; }
    public void setEnablePathMerging(boolean enablePathMerging) { this.enablePathMerging = enablePathMerging; }
    
    public boolean isEnablePathPruning() { return enablePathPruning; }
    public void setEnablePathPruning(boolean enablePathPruning) { this.enablePathPruning = enablePathPruning; }
    
    // 性能优化配置的getter和setter
    public int getThreadPoolSize() { return threadPoolSize; }
    public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
    
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    
    public boolean isEnableCaching() { return enableCaching; }
    public void setEnableCaching(boolean enableCaching) { this.enableCaching = enableCaching; }
    
    public int getCacheSize() { return cacheSize; }
    public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
    
    // 质量控制配置的getter和setter
    public double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public boolean isStrictValidation() { return strictValidation; }
    public void setStrictValidation(boolean strictValidation) { this.strictValidation = strictValidation; }
    
    /**
     * 创建默认配置
     */
    public static ReasoningConfig createDefault() {
        return new ReasoningConfig();
    }
    
    /**
     * 创建高性能配置
     */
    public static ReasoningConfig createHighPerformance() {
        ReasoningConfig config = new ReasoningConfig();
        config.setMaxDepth(2);
        config.setWidth(5);
        config.setMaxEntities(50);
        config.setMaxPaths(20);
        config.setThreadPoolSize(8);
        config.setBatchSize(20);
        return config;
    }
    
    /**
     * 创建高质量配置
     */
    public static ReasoningConfig createHighQuality() {
        ReasoningConfig config = new ReasoningConfig();
        config.setMaxDepth(4);
        config.setWidth(3);
        config.setMaxEntities(200);
        config.setMaxPaths(100);
        config.setConfidenceThreshold(0.5);
        config.setStrictValidation(true);
        return config;
    }
    
    /**
     * 验证配置的有效性
     */
    public boolean isValid() {
        return maxDepth > 0 && 
               width > 0 && 
               maxEntities > 0 && 
               maxPaths > 0 && 
               entityThreshold >= 0.0 && entityThreshold <= 1.0 &&
               relationThreshold >= 0.0 && relationThreshold <= 1.0 &&
               confidenceThreshold >= 0.0 && confidenceThreshold <= 1.0;
    }
    
    /**
     * 获取配置摘要
     */
    public String getSummary() {
        return String.format("ReasoningConfig[depth=%d, width=%d, maxEntities=%d, maxPaths=%d, " +
                           "entityThreshold=%.2f, relationThreshold=%.2f, confidenceThreshold=%.2f]",
                           maxDepth, width, maxEntities, maxPaths, 
                           entityThreshold, relationThreshold, confidenceThreshold);
    }
}