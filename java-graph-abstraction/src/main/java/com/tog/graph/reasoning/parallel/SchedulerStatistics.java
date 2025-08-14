package com.tog.graph.reasoning.parallel;

import java.util.HashMap;
import java.util.Map;

/**
 * 调度器统计信息
 * 收集和分析任务调度的性能数据
 */
public class SchedulerStatistics {
    
    private final long totalTasksExecuted;
    private final long totalExecutionTime;
    private final int activeTaskCount;
    private final int queuedTaskCount;
    private final Map<ReasoningTaskScheduler.TaskType, TaskMetrics> taskMetrics;
    private final ResourceMonitor.ResourceMetrics resourceMetrics;
    
    // 计算得出的指标
    private final double averageExecutionTime;
    private final double throughput;
    private final double queueUtilization;
    
    public SchedulerStatistics(long totalTasksExecuted, long totalExecutionTime,
                              int activeTaskCount, int queuedTaskCount,
                              Map<ReasoningTaskScheduler.TaskType, TaskMetrics> taskMetrics,
                              ResourceMonitor.ResourceMetrics resourceMetrics) {
        this.totalTasksExecuted = totalTasksExecuted;
        this.totalExecutionTime = totalExecutionTime;
        this.activeTaskCount = activeTaskCount;
        this.queuedTaskCount = queuedTaskCount;
        this.taskMetrics = new HashMap<>(taskMetrics);
        this.resourceMetrics = resourceMetrics;
        
        // 计算派生指标
        this.averageExecutionTime = totalTasksExecuted > 0 ? 
                                   (double) totalExecutionTime / totalTasksExecuted : 0.0;
        this.throughput = averageExecutionTime > 0 ? 1000.0 / averageExecutionTime : 0.0;
        this.queueUtilization = (double) queuedTaskCount / (activeTaskCount + queuedTaskCount + 1);
    }
    
    /**
     * 获取整体性能评级
     */
    public String getPerformanceRating() {
        if (throughput >= 50) return "Excellent";
        if (throughput >= 20) return "Good";
        if (throughput >= 10) return "Fair";
        if (throughput >= 5) return "Poor";
        return "Very Poor";
    }
    
    /**
     * 获取系统健康状态
     */
    public String getHealthStatus() {
        if (resourceMetrics == null) return "Unknown";
        
        double cpuUsage = resourceMetrics.getCpuUsage();
        double memoryUsage = resourceMetrics.getMemoryUsage();
        
        if (cpuUsage > 0.9 || memoryUsage > 0.9) return "Critical";
        if (cpuUsage > 0.8 || memoryUsage > 0.8) return "Warning";
        if (cpuUsage > 0.6 || memoryUsage > 0.6) return "Moderate";
        return "Healthy";
    }
    
    /**
     * 获取队列状态
     */
    public String getQueueStatus() {
        if (queuedTaskCount == 0) return "Empty";
        if (queueUtilization < 0.3) return "Light";
        if (queueUtilization < 0.6) return "Moderate";
        if (queueUtilization < 0.8) return "Heavy";
        return "Overloaded";
    }
    
    /**
     * 获取最繁忙的任务类型
     */
    public ReasoningTaskScheduler.TaskType getBusiestTaskType() {
        return taskMetrics.entrySet().stream()
                .max((e1, e2) -> Integer.compare(e1.getValue().getExecutionCount(), 
                                               e2.getValue().getExecutionCount()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * 获取最慢的任务类型
     */
    public ReasoningTaskScheduler.TaskType getSlowestTaskType() {
        return taskMetrics.entrySet().stream()
                .max((e1, e2) -> Double.compare(e1.getValue().getAverageExecutionTime(), 
                                              e2.getValue().getAverageExecutionTime()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * 获取成功率最低的任务类型
     */
    public ReasoningTaskScheduler.TaskType getLeastReliableTaskType() {
        return taskMetrics.entrySet().stream()
                .min((e1, e2) -> Double.compare(e1.getValue().getSuccessRate(), 
                                              e2.getValue().getSuccessRate()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * 获取统计摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Scheduler Statistics Summary:\n");
        summary.append(String.format("  Total Tasks: %d\n", totalTasksExecuted));
        summary.append(String.format("  Average Execution Time: %.2fms\n", averageExecutionTime));
        summary.append(String.format("  Throughput: %.2f tasks/sec\n", throughput));
        summary.append(String.format("  Performance Rating: %s\n", getPerformanceRating()));
        summary.append(String.format("  Active Tasks: %d\n", activeTaskCount));
        summary.append(String.format("  Queued Tasks: %d\n", queuedTaskCount));
        summary.append(String.format("  Queue Status: %s\n", getQueueStatus()));
        summary.append(String.format("  Health Status: %s\n", getHealthStatus()));
        
        if (resourceMetrics != null) {
            summary.append(String.format("  CPU Usage: %.1f%%\n", resourceMetrics.getCpuUsage() * 100));
            summary.append(String.format("  Memory Usage: %.1f%%\n", resourceMetrics.getMemoryUsage() * 100));
            summary.append(String.format("  Thread Count: %d\n", resourceMetrics.getThreadCount()));
        }
        
        return summary.toString();
    }
    
    /**
     * 获取详细报告
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append(getSummary());
        report.append("\n");
        
        // 任务类型详细统计
        if (!taskMetrics.isEmpty()) {
            report.append("Task Type Statistics:\n");
            taskMetrics.forEach((type, metrics) -> {
                report.append(String.format("  %s:\n", type));
                report.append(String.format("    Executions: %d\n", metrics.getExecutionCount()));
                report.append(String.format("    Success Rate: %.1f%%\n", metrics.getSuccessRate() * 100));
                report.append(String.format("    Avg Time: %.2fms\n", metrics.getAverageExecutionTime()));
                report.append(String.format("    Min Time: %dms\n", metrics.getMinExecutionTime()));
                report.append(String.format("    Max Time: %dms\n", metrics.getMaxExecutionTime()));
                report.append(String.format("    Throughput: %.2f/sec\n", metrics.getThroughput()));
                report.append("\n");
            });
        }
        
        // 资源使用详情
        if (resourceMetrics != null) {
            report.append("Resource Usage Details:\n");
            report.append(String.format("  CPU Usage: %.1f%%\n", resourceMetrics.getCpuUsage() * 100));
            report.append(String.format("  Memory: %.1fMB / %.1fMB (%.1f%%)\n",
                         resourceMetrics.getUsedMemoryMB(),
                         resourceMetrics.getMaxMemoryMB(),
                         resourceMetrics.getMemoryUsage() * 100));
            report.append(String.format("  Threads: %d (Peak: %d)\n",
                         resourceMetrics.getThreadCount(),
                         resourceMetrics.getPeakThreadCount()));
            report.append(String.format("  System Load: %.2f\n", resourceMetrics.getSystemLoad()));
            report.append(String.format("  Available Processors: %d\n", resourceMetrics.getAvailableProcessors()));
        }
        
        // 性能建议
        report.append("\nPerformance Recommendations:\n");
        report.append(generateRecommendations());
        
        return report.toString();
    }
    
    /**
     * 生成性能建议
     */
    private String generateRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        
        // 基于吞吐量的建议
        if (throughput < 10) {
            recommendations.append("  - Consider increasing thread pool size for better throughput\n");
        }
        
        // 基于队列状态的建议
        if (queuedTaskCount > activeTaskCount * 2) {
            recommendations.append("  - High queue backlog detected, consider scaling up resources\n");
        }
        
        // 基于资源使用的建议
        if (resourceMetrics != null) {
            if (resourceMetrics.getCpuUsage() > 0.8) {
                recommendations.append("  - High CPU usage detected, consider optimizing CPU-intensive tasks\n");
            }
            
            if (resourceMetrics.getMemoryUsage() > 0.8) {
                recommendations.append("  - High memory usage detected, consider memory optimization\n");
            }
            
            if (resourceMetrics.getThreadCount() > 100) {
                recommendations.append("  - High thread count detected, consider thread pool tuning\n");
            }
        }
        
        // 基于任务类型的建议
        ReasoningTaskScheduler.TaskType slowestType = getSlowestTaskType();
        if (slowestType != null) {
            TaskMetrics slowestMetrics = taskMetrics.get(slowestType);
            if (slowestMetrics.getAverageExecutionTime() > 1000) {
                recommendations.append(String.format("  - %s tasks are slow (%.2fms avg), consider optimization\n",
                                     slowestType, slowestMetrics.getAverageExecutionTime()));
            }
        }
        
        ReasoningTaskScheduler.TaskType leastReliableType = getLeastReliableTaskType();
        if (leastReliableType != null) {
            TaskMetrics leastReliableMetrics = taskMetrics.get(leastReliableType);
            if (leastReliableMetrics.getSuccessRate() < 0.9) {
                recommendations.append(String.format("  - %s tasks have low success rate (%.1f%%), investigate failures\n",
                                     leastReliableType, leastReliableMetrics.getSuccessRate() * 100));
            }
        }
        
        if (recommendations.length() == 0) {
            recommendations.append("  - System performance is optimal, no recommendations at this time\n");
        }
        
        return recommendations.toString();
    }
    
    /**
     * 导出为Map格式
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalTasksExecuted", totalTasksExecuted);
        map.put("totalExecutionTime", totalExecutionTime);
        map.put("activeTaskCount", activeTaskCount);
        map.put("queuedTaskCount", queuedTaskCount);
        map.put("averageExecutionTime", averageExecutionTime);
        map.put("throughput", throughput);
        map.put("queueUtilization", queueUtilization);
        map.put("performanceRating", getPerformanceRating());
        map.put("healthStatus", getHealthStatus());
        map.put("queueStatus", getQueueStatus());
        
        if (resourceMetrics != null) {
            map.put("cpuUsage", resourceMetrics.getCpuUsage());
            map.put("memoryUsage", resourceMetrics.getMemoryUsage());
            map.put("threadCount", resourceMetrics.getThreadCount());
        }
        
        return map;
    }
    
    // Getters
    public long getTotalTasksExecuted() { return totalTasksExecuted; }
    public long getTotalExecutionTime() { return totalExecutionTime; }
    public int getActiveTaskCount() { return activeTaskCount; }
    public int getQueuedTaskCount() { return queuedTaskCount; }
    public Map<ReasoningTaskScheduler.TaskType, TaskMetrics> getTaskMetrics() { 
        return new HashMap<>(taskMetrics); 
    }
    public ResourceMonitor.ResourceMetrics getResourceMetrics() { return resourceMetrics; }
    public double getAverageExecutionTime() { return averageExecutionTime; }
    public double getThroughput() { return throughput; }
    public double getQueueUtilization() { return queueUtilization; }
    
    @Override
    public String toString() {
        return String.format("SchedulerStatistics[tasks=%d, throughput=%.2f/s, active=%d, queued=%d, health=%s]",
                           totalTasksExecuted, throughput, activeTaskCount, queuedTaskCount, getHealthStatus());
    }
}