package com.tog.graph.reasoning.parallel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务执行指标
 * 记录和统计任务执行的性能数据
 */
public class TaskMetrics {
    
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTime = new AtomicLong(0);
    
    // 最近的执行时间（用于计算移动平均）
    private final long[] recentExecutionTimes = new long[10];
    private volatile int recentIndex = 0;
    
    /**
     * 记录任务执行
     */
    public void recordExecution(long executionTime, boolean success) {
        executionCount.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        
        if (success) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }
        
        // 更新最小和最大执行时间
        updateMinTime(executionTime);
        updateMaxTime(executionTime);
        
        // 记录最近的执行时间
        synchronized (recentExecutionTimes) {
            recentExecutionTimes[recentIndex] = executionTime;
            recentIndex = (recentIndex + 1) % recentExecutionTimes.length;
        }
    }
    
    /**
     * 更新最小执行时间
     */
    private void updateMinTime(long executionTime) {
        long currentMin = minExecutionTime.get();
        while (executionTime < currentMin) {
            if (minExecutionTime.compareAndSet(currentMin, executionTime)) {
                break;
            }
            currentMin = minExecutionTime.get();
        }
    }
    
    /**
     * 更新最大执行时间
     */
    private void updateMaxTime(long executionTime) {
        long currentMax = maxExecutionTime.get();
        while (executionTime > currentMax) {
            if (maxExecutionTime.compareAndSet(currentMax, executionTime)) {
                break;
            }
            currentMax = maxExecutionTime.get();
        }
    }
    
    /**
     * 获取执行次数
     */
    public int getExecutionCount() {
        return executionCount.get();
    }
    
    /**
     * 获取成功次数
     */
    public int getSuccessCount() {
        return successCount.get();
    }
    
    /**
     * 获取失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        int total = executionCount.get();
        return total > 0 ? (double) successCount.get() / total : 0.0;
    }
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        return 1.0 - getSuccessRate();
    }
    
    /**
     * 获取平均执行时间
     */
    public double getAverageExecutionTime() {
        int count = executionCount.get();
        return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
    }
    
    /**
     * 获取最小执行时间
     */
    public long getMinExecutionTime() {
        long min = minExecutionTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    /**
     * 获取最大执行时间
     */
    public long getMaxExecutionTime() {
        return maxExecutionTime.get();
    }
    
    /**
     * 获取最近的平均执行时间
     */
    public double getRecentAverageExecutionTime() {
        synchronized (recentExecutionTimes) {
            long sum = 0;
            int count = 0;
            
            for (long time : recentExecutionTimes) {
                if (time > 0) {
                    sum += time;
                    count++;
                }
            }
            
            return count > 0 ? (double) sum / count : 0.0;
        }
    }
    
    /**
     * 获取吞吐量（每秒执行次数）
     */
    public double getThroughput() {
        double avgTime = getAverageExecutionTime();
        return avgTime > 0 ? 1000.0 / avgTime : 0.0;
    }
    
    /**
     * 重置指标
     */
    public void reset() {
        executionCount.set(0);
        successCount.set(0);
        failureCount.set(0);
        totalExecutionTime.set(0);
        minExecutionTime.set(Long.MAX_VALUE);
        maxExecutionTime.set(0);
        
        synchronized (recentExecutionTimes) {
            for (int i = 0; i < recentExecutionTimes.length; i++) {
                recentExecutionTimes[i] = 0;
            }
            recentIndex = 0;
        }
    }
    
    /**
     * 获取指标摘要
     */
    public String getSummary() {
        return String.format(
            "TaskMetrics[count=%d, success=%.1f%%, avgTime=%.2fms, minTime=%dms, maxTime=%dms, throughput=%.2f/s]",
            getExecutionCount(),
            getSuccessRate() * 100,
            getAverageExecutionTime(),
            getMinExecutionTime(),
            getMaxExecutionTime(),
            getThroughput()
        );
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}