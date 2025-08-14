package com.tog.graph.reasoning;

import java.util.HashMap;
import java.util.Map;

/**
 * 推理统计信息
 * 收集和分析推理过程的性能指标
 */
public class ReasoningStatistics {
    private final String question;
    private final long startTime;
    private final long duration;
    private final int maxDepthReached;
    private final int totalEntitiesExplored;
    private final int totalRelationsExplored;
    private final int totalPathsFound;
    private final int totalEntitiesAcrossDepths;
    
    // 详细统计
    private final Map<String, Long> timingStats;
    private final Map<String, Integer> countStats;
    
    // 计算得出的指标
    private final double explorationEfficiency;
    private final double pathDiscoveryRate;
    private final double averagePathLength;
    
    public ReasoningStatistics(String question, long startTime, long duration, int maxDepthReached,
                             int totalEntitiesExplored, int totalRelationsExplored, int totalPathsFound,
                             int totalEntitiesAcrossDepths, Map<String, Long> timingStats, 
                             Map<String, Integer> countStats) {
        this.question = question;
        this.startTime = startTime;
        this.duration = duration;
        this.maxDepthReached = maxDepthReached;
        this.totalEntitiesExplored = totalEntitiesExplored;
        this.totalRelationsExplored = totalRelationsExplored;
        this.totalPathsFound = totalPathsFound;
        this.totalEntitiesAcrossDepths = totalEntitiesAcrossDepths;
        this.timingStats = new HashMap<>(timingStats);
        this.countStats = new HashMap<>(countStats);
        
        // 计算派生指标
        this.explorationEfficiency = calculateExplorationEfficiency();
        this.pathDiscoveryRate = calculatePathDiscoveryRate();
        this.averagePathLength = calculateAveragePathLength();
    }
    
    /**
     * 计算探索效率
     */
    private double calculateExplorationEfficiency() {
        if (totalEntitiesExplored == 0) return 0.0;
        return (double) totalPathsFound / totalEntitiesExplored;
    }
    
    /**
     * 计算路径发现率
     */
    private double calculatePathDiscoveryRate() {
        if (totalRelationsExplored == 0) return 0.0;
        return (double) totalPathsFound / totalRelationsExplored;
    }
    
    /**
     * 计算平均路径长度
     */
    private double calculateAveragePathLength() {
        if (totalPathsFound == 0) return 0.0;
        // 这里简化处理，实际应该从路径数据中计算
        return maxDepthReached > 0 ? (double) maxDepthReached / 2.0 : 0.0;
    }
    
    /**
     * 获取每秒处理的实体数
     */
    public double getEntitiesPerSecond() {
        if (duration == 0) return 0.0;
        return (double) totalEntitiesExplored / (duration / 1000.0);
    }
    
    /**
     * 获取每秒处理的关系数
     */
    public double getRelationsPerSecond() {
        if (duration == 0) return 0.0;
        return (double) totalRelationsExplored / (duration / 1000.0);
    }
    
    /**
     * 获取内存使用估算
     */
    public long getEstimatedMemoryUsage() {
        // 简化的内存使用估算
        long entityMemory = totalEntitiesAcrossDepths * 200; // 每个实体约200字节
        long relationMemory = totalRelationsExplored * 150;  // 每个关系约150字节
        long pathMemory = totalPathsFound * 300;             // 每个路径约300字节
        
        return entityMemory + relationMemory + pathMemory;
    }
    
    /**
     * 获取性能评级
     */
    public String getPerformanceRating() {
        double entitiesPerSec = getEntitiesPerSecond();
        
        if (entitiesPerSec >= 100) return "Excellent";
        if (entitiesPerSec >= 50) return "Good";
        if (entitiesPerSec >= 20) return "Fair";
        if (entitiesPerSec >= 10) return "Poor";
        return "Very Poor";
    }
    
    /**
     * 获取效率评级
     */
    public String getEfficiencyRating() {
        if (explorationEfficiency >= 0.5) return "Highly Efficient";
        if (explorationEfficiency >= 0.3) return "Efficient";
        if (explorationEfficiency >= 0.1) return "Moderately Efficient";
        if (explorationEfficiency >= 0.05) return "Low Efficiency";
        return "Very Low Efficiency";
    }
    
    /**
     * 获取统计摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Reasoning Statistics Summary:\n"));
        summary.append(String.format("  Duration: %dms\n", duration));
        summary.append(String.format("  Max Depth: %d\n", maxDepthReached));
        summary.append(String.format("  Entities Explored: %d\n", totalEntitiesExplored));
        summary.append(String.format("  Relations Explored: %d\n", totalRelationsExplored));
        summary.append(String.format("  Paths Found: %d\n", totalPathsFound));
        summary.append(String.format("  Exploration Efficiency: %.3f\n", explorationEfficiency));
        summary.append(String.format("  Path Discovery Rate: %.3f\n", pathDiscoveryRate));
        summary.append(String.format("  Performance Rating: %s\n", getPerformanceRating()));
        summary.append(String.format("  Efficiency Rating: %s\n", getEfficiencyRating()));
        
        return summary.toString();
    }
    
    /**
     * 获取详细报告
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append(getSummary());
        report.append("\n");
        
        // 性能指标
        report.append("Performance Metrics:\n");
        report.append(String.format("  Entities/sec: %.2f\n", getEntitiesPerSecond()));
        report.append(String.format("  Relations/sec: %.2f\n", getRelationsPerSecond()));
        report.append(String.format("  Estimated Memory: %d bytes\n", getEstimatedMemoryUsage()));
        report.append(String.format("  Average Path Length: %.2f\n", averagePathLength));
        
        // 时间统计
        if (!timingStats.isEmpty()) {
            report.append("\nTiming Breakdown:\n");
            timingStats.forEach((operation, time) -> 
                report.append(String.format("  %s: %dms\n", operation, time)));
        }
        
        // 计数统计
        if (!countStats.isEmpty()) {
            report.append("\nCount Statistics:\n");
            countStats.forEach((metric, count) -> 
                report.append(String.format("  %s: %d\n", metric, count)));
        }
        
        return report.toString();
    }
    
    /**
     * 与另一个统计进行比较
     */
    public String compareWith(ReasoningStatistics other) {
        if (other == null) return "No comparison available";
        
        StringBuilder comparison = new StringBuilder();
        comparison.append("Performance Comparison:\n");
        
        double durationRatio = (double) this.duration / other.duration;
        double entityRatio = (double) this.totalEntitiesExplored / other.totalEntitiesExplored;
        double pathRatio = (double) this.totalPathsFound / other.totalPathsFound;
        
        comparison.append(String.format("  Duration: %.2fx %s\n", durationRatio, 
                         durationRatio < 1.0 ? "faster" : "slower"));
        comparison.append(String.format("  Entities Explored: %.2fx %s\n", entityRatio,
                         entityRatio > 1.0 ? "more" : "fewer"));
        comparison.append(String.format("  Paths Found: %.2fx %s\n", pathRatio,
                         pathRatio > 1.0 ? "more" : "fewer"));
        
        return comparison.toString();
    }
    
    /**
     * 导出为Map格式
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("question", question);
        map.put("startTime", startTime);
        map.put("duration", duration);
        map.put("maxDepthReached", maxDepthReached);
        map.put("totalEntitiesExplored", totalEntitiesExplored);
        map.put("totalRelationsExplored", totalRelationsExplored);
        map.put("totalPathsFound", totalPathsFound);
        map.put("explorationEfficiency", explorationEfficiency);
        map.put("pathDiscoveryRate", pathDiscoveryRate);
        map.put("averagePathLength", averagePathLength);
        map.put("entitiesPerSecond", getEntitiesPerSecond());
        map.put("relationsPerSecond", getRelationsPerSecond());
        map.put("estimatedMemoryUsage", getEstimatedMemoryUsage());
        map.put("performanceRating", getPerformanceRating());
        map.put("efficiencyRating", getEfficiencyRating());
        
        return map;
    }
    
    // Getters
    public String getQuestion() { return question; }
    public long getStartTime() { return startTime; }
    public long getDuration() { return duration; }
    public int getMaxDepthReached() { return maxDepthReached; }
    public int getTotalEntitiesExplored() { return totalEntitiesExplored; }
    public int getTotalRelationsExplored() { return totalRelationsExplored; }
    public int getTotalPathsFound() { return totalPathsFound; }
    public int getTotalEntitiesAcrossDepths() { return totalEntitiesAcrossDepths; }
    public Map<String, Long> getTimingStats() { return new HashMap<>(timingStats); }
    public Map<String, Integer> getCountStats() { return new HashMap<>(countStats); }
    public double getExplorationEfficiency() { return explorationEfficiency; }
    public double getPathDiscoveryRate() { return pathDiscoveryRate; }
    public double getAveragePathLength() { return averagePathLength; }
    
    @Override
    public String toString() {
        return String.format("ReasoningStatistics[duration=%dms, entities=%d, relations=%d, paths=%d, efficiency=%.3f]",
                           duration, totalEntitiesExplored, totalRelationsExplored, totalPathsFound, explorationEfficiency);
    }
}