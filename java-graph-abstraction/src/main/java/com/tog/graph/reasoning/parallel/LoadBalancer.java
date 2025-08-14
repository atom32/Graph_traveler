package com.tog.graph.reasoning.parallel;

import com.tog.graph.reasoning.ReasoningConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡器
 * 智能分配任务到不同的执行器
 */
public class LoadBalancer {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);
    
    private final ReasoningConfig config;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    
    // 负载统计
    private final AtomicInteger mainExecutorLoad = new AtomicInteger(0);
    private final AtomicInteger ioExecutorLoad = new AtomicInteger(0);
    
    public LoadBalancer(ReasoningConfig config) {
        this.config = config;
    }
    
    /**
     * 选择最佳执行器
     */
    public ExecutorType selectExecutor(ReasoningTaskScheduler.TaskType taskType) {
        return switch (taskType) {
            case DATABASE_QUERY, EMBEDDING_CALCULATION -> {
                // IO密集型任务优先使用IO执行器
                if (ioExecutorLoad.get() < config.getThreadPoolSize() / 2) {
                    ioExecutorLoad.incrementAndGet();
                    yield ExecutorType.IO_EXECUTOR;
                } else {
                    // IO执行器满载，使用主执行器
                    mainExecutorLoad.incrementAndGet();
                    yield ExecutorType.MAIN_EXECUTOR;
                }
            }
            
            case GRAPH_TRAVERSAL, PATH_SCORING, RESULT_AGGREGATION -> {
                // CPU密集型任务优先使用主执行器
                if (mainExecutorLoad.get() < config.getThreadPoolSize()) {
                    mainExecutorLoad.incrementAndGet();
                    yield ExecutorType.MAIN_EXECUTOR;
                } else {
                    // 主执行器满载，使用轮询策略
                    yield selectByRoundRobin();
                }
            }
            
            case LLM_GENERATION -> {
                // LLM任务通常是IO密集型，但可能需要较长时间
                yield selectByLoad();
            }
            
            default -> ExecutorType.MAIN_EXECUTOR;
        };
    }
    
    /**
     * 基于负载选择执行器
     */
    private ExecutorType selectByLoad() {
        int mainLoad = mainExecutorLoad.get();
        int ioLoad = ioExecutorLoad.get();
        
        if (ioLoad < mainLoad) {
            ioExecutorLoad.incrementAndGet();
            return ExecutorType.IO_EXECUTOR;
        } else {
            mainExecutorLoad.incrementAndGet();
            return ExecutorType.MAIN_EXECUTOR;
        }
    }
    
    /**
     * 轮询选择执行器
     */
    private ExecutorType selectByRoundRobin() {
        int counter = roundRobinCounter.getAndIncrement();
        
        if (counter % 2 == 0) {
            mainExecutorLoad.incrementAndGet();
            return ExecutorType.MAIN_EXECUTOR;
        } else {
            ioExecutorLoad.incrementAndGet();
            return ExecutorType.IO_EXECUTOR;
        }
    }
    
    /**
     * 任务完成时释放负载
     */
    public void releaseLoad(ExecutorType executorType) {
        switch (executorType) {
            case MAIN_EXECUTOR -> mainExecutorLoad.decrementAndGet();
            case IO_EXECUTOR -> ioExecutorLoad.decrementAndGet();
        }
    }
    
    /**
     * 获取当前负载情况
     */
    public LoadInfo getCurrentLoad() {
        return new LoadInfo(
            mainExecutorLoad.get(),
            ioExecutorLoad.get(),
            config.getThreadPoolSize(),
            config.getThreadPoolSize() / 2
        );
    }
    
    /**
     * 检查是否高负载
     */
    public boolean isHighLoad() {
        int totalLoad = mainExecutorLoad.get() + ioExecutorLoad.get();
        int totalCapacity = config.getThreadPoolSize() + config.getThreadPoolSize() / 2;
        
        return (double) totalLoad / totalCapacity > 0.8; // 80%负载阈值
    }
    
    /**
     * 重置负载统计
     */
    public void resetLoad() {
        mainExecutorLoad.set(0);
        ioExecutorLoad.set(0);
        roundRobinCounter.set(0);
    }
    
    /**
     * 执行器类型枚举
     */
    public enum ExecutorType {
        MAIN_EXECUTOR,
        IO_EXECUTOR
    }
    
    /**
     * 负载信息类
     */
    public static class LoadInfo {
        private final int mainExecutorLoad;
        private final int ioExecutorLoad;
        private final int mainExecutorCapacity;
        private final int ioExecutorCapacity;
        
        public LoadInfo(int mainExecutorLoad, int ioExecutorLoad, 
                       int mainExecutorCapacity, int ioExecutorCapacity) {
            this.mainExecutorLoad = mainExecutorLoad;
            this.ioExecutorLoad = ioExecutorLoad;
            this.mainExecutorCapacity = mainExecutorCapacity;
            this.ioExecutorCapacity = ioExecutorCapacity;
        }
        
        public int getMainExecutorLoad() { return mainExecutorLoad; }
        public int getIoExecutorLoad() { return ioExecutorLoad; }
        public int getMainExecutorCapacity() { return mainExecutorCapacity; }
        public int getIoExecutorCapacity() { return ioExecutorCapacity; }
        
        public double getMainExecutorUtilization() {
            return mainExecutorCapacity > 0 ? (double) mainExecutorLoad / mainExecutorCapacity : 0.0;
        }
        
        public double getIoExecutorUtilization() {
            return ioExecutorCapacity > 0 ? (double) ioExecutorLoad / ioExecutorCapacity : 0.0;
        }
        
        public double getOverallUtilization() {
            int totalLoad = mainExecutorLoad + ioExecutorLoad;
            int totalCapacity = mainExecutorCapacity + ioExecutorCapacity;
            return totalCapacity > 0 ? (double) totalLoad / totalCapacity : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("LoadInfo[main=%d/%d(%.1f%%), io=%d/%d(%.1f%%), overall=%.1f%%]",
                               mainExecutorLoad, mainExecutorCapacity, getMainExecutorUtilization() * 100,
                               ioExecutorLoad, ioExecutorCapacity, getIoExecutorUtilization() * 100,
                               getOverallUtilization() * 100);
        }
    }
}