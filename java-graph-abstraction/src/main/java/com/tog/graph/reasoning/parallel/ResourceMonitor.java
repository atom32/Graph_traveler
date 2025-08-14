package com.tog.graph.reasoning.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 资源监控器
 * 监控系统资源使用情况，用于负载均衡和任务调度决策
 */
public class ResourceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);
    
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;
    
    private final AtomicReference<ResourceMetrics> currentMetrics = new AtomicReference<>();
    
    // 阈值配置
    private static final double HIGH_CPU_THRESHOLD = 0.8;      // 80% CPU使用率
    private static final double HIGH_MEMORY_THRESHOLD = 0.85;  // 85% 内存使用率
    private static final int HIGH_THREAD_THRESHOLD = 200;      // 200个线程
    
    public ResourceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        
        // 初始化指标
        updateMetrics();
    }
    
    /**
     * 更新资源指标
     */
    public void updateMetrics() {
        try {
            // CPU使用率
            double cpuUsage = getCpuUsage();
            
            // 内存使用情况
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = maxMemory > 0 ? (double) usedMemory / maxMemory : 0.0;
            
            // 线程信息
            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            
            // 系统负载
            double systemLoad = osBean.getSystemLoadAverage();
            int availableProcessors = osBean.getAvailableProcessors();
            
            ResourceMetrics metrics = new ResourceMetrics(
                cpuUsage,
                memoryUsage,
                usedMemory,
                maxMemory,
                threadCount,
                peakThreadCount,
                systemLoad,
                availableProcessors,
                System.currentTimeMillis()
            );
            
            currentMetrics.set(metrics);
            
            // 记录高负载警告
            if (isHighLoad()) {
                logger.warn("High system load detected: {}", metrics.getSummary());
            }
            
        } catch (Exception e) {
            logger.error("Failed to update resource metrics", e);
        }
    }
    
    /**
     * 获取CPU使用率
     */
    private double getCpuUsage() {
        try {
            // 尝试获取进程CPU使用率
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double processCpuLoad = sunOsBean.getProcessCpuLoad();
                return processCpuLoad >= 0 ? processCpuLoad : 0.0;
            }
            
            // 回退到系统负载平均值
            double systemLoad = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            
            if (systemLoad >= 0 && processors > 0) {
                return Math.min(1.0, systemLoad / processors);
            }
            
            return 0.0;
            
        } catch (Exception e) {
            logger.debug("Failed to get CPU usage", e);
            return 0.0;
        }
    }
    
    /**
     * 检查是否为高负载状态
     */
    public boolean isHighLoad() {
        ResourceMetrics metrics = currentMetrics.get();
        if (metrics == null) return false;
        
        return metrics.getCpuUsage() > HIGH_CPU_THRESHOLD ||
               metrics.getMemoryUsage() > HIGH_MEMORY_THRESHOLD ||
               metrics.getThreadCount() > HIGH_THREAD_THRESHOLD;
    }
    
    /**
     * 检查是否为高CPU负载
     */
    public boolean isHighCpuLoad() {
        ResourceMetrics metrics = currentMetrics.get();
        return metrics != null && metrics.getCpuUsage() > HIGH_CPU_THRESHOLD;
    }
    
    /**
     * 检查是否为高内存负载
     */
    public boolean isHighMemoryLoad() {
        ResourceMetrics metrics = currentMetrics.get();
        return metrics != null && metrics.getMemoryUsage() > HIGH_MEMORY_THRESHOLD;
    }
    
    /**
     * 获取当前资源指标
     */
    public ResourceMetrics getCurrentMetrics() {
        return currentMetrics.get();
    }
    
    /**
     * 获取负载评级
     */
    public LoadLevel getLoadLevel() {
        ResourceMetrics metrics = currentMetrics.get();
        if (metrics == null) return LoadLevel.UNKNOWN;
        
        double maxUsage = Math.max(metrics.getCpuUsage(), metrics.getMemoryUsage());
        
        if (maxUsage < 0.3) return LoadLevel.LOW;
        if (maxUsage < 0.6) return LoadLevel.MEDIUM;
        if (maxUsage < 0.8) return LoadLevel.HIGH;
        return LoadLevel.CRITICAL;
    }
    
    /**
     * 获取建议的线程池大小
     */
    public int getRecommendedThreadPoolSize() {
        ResourceMetrics metrics = currentMetrics.get();
        if (metrics == null) return Runtime.getRuntime().availableProcessors();
        
        int processors = metrics.getAvailableProcessors();
        double cpuUsage = metrics.getCpuUsage();
        double memoryUsage = metrics.getMemoryUsage();
        
        // 基于资源使用情况调整线程池大小
        if (cpuUsage > 0.8 || memoryUsage > 0.8) {
            return Math.max(1, processors / 2); // 高负载时减少线程
        } else if (cpuUsage < 0.3 && memoryUsage < 0.5) {
            return processors * 2; // 低负载时增加线程
        } else {
            return processors; // 正常负载
        }
    }
    
    /**
     * 资源指标类
     */
    public static class ResourceMetrics {
        private final double cpuUsage;
        private final double memoryUsage;
        private final long usedMemory;
        private final long maxMemory;
        private final int threadCount;
        private final int peakThreadCount;
        private final double systemLoad;
        private final int availableProcessors;
        private final long timestamp;
        
        public ResourceMetrics(double cpuUsage, double memoryUsage, long usedMemory, long maxMemory,
                              int threadCount, int peakThreadCount, double systemLoad, 
                              int availableProcessors, long timestamp) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.threadCount = threadCount;
            this.peakThreadCount = peakThreadCount;
            this.systemLoad = systemLoad;
            this.availableProcessors = availableProcessors;
            this.timestamp = timestamp;
        }
        
        // Getters
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public int getThreadCount() { return threadCount; }
        public int getPeakThreadCount() { return peakThreadCount; }
        public double getSystemLoad() { return systemLoad; }
        public int getAvailableProcessors() { return availableProcessors; }
        public long getTimestamp() { return timestamp; }
        
        /**
         * 获取可用内存（字节）
         */
        public long getAvailableMemory() {
            return maxMemory - usedMemory;
        }
        
        /**
         * 获取内存使用量（MB）
         */
        public double getUsedMemoryMB() {
            return usedMemory / (1024.0 * 1024.0);
        }
        
        /**
         * 获取最大内存（MB）
         */
        public double getMaxMemoryMB() {
            return maxMemory / (1024.0 * 1024.0);
        }
        
        /**
         * 获取摘要信息
         */
        public String getSummary() {
            return String.format(
                "ResourceMetrics[CPU=%.1f%%, Memory=%.1f%%(%.1f/%.1fMB), Threads=%d, SystemLoad=%.2f]",
                cpuUsage * 100,
                memoryUsage * 100,
                getUsedMemoryMB(),
                getMaxMemoryMB(),
                threadCount,
                systemLoad
            );
        }
        
        @Override
        public String toString() {
            return getSummary();
        }
    }
    
    /**
     * 负载级别枚举
     */
    public enum LoadLevel {
        LOW,        // 低负载
        MEDIUM,     // 中等负载
        HIGH,       // 高负载
        CRITICAL,   // 临界负载
        UNKNOWN     // 未知
    }
}