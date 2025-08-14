package com.tog.graph.reasoning.parallel;

import com.tog.graph.reasoning.ReasoningConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 推理任务调度器
 * 智能管理并行推理任务的执行和资源分配
 */
public class ReasoningTaskScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReasoningTaskScheduler.class);
    
    private final ExecutorService mainExecutor;
    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final ReasoningConfig config;
    
    // 任务管理
    private final Map<String, ReasoningTask<?>> activeTasks = new ConcurrentHashMap<>();
    private final Queue<ReasoningTask<?>> pendingTasks = new ConcurrentLinkedQueue<>();
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    
    // 性能监控
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final Map<TaskType, TaskMetrics> taskMetrics = new ConcurrentHashMap<>();
    
    // 负载均衡
    private final LoadBalancer loadBalancer;
    private final ResourceMonitor resourceMonitor;
    
    public ReasoningTaskScheduler(ReasoningConfig config) {
        this.config = config;
        
        // 创建线程池
        this.mainExecutor = createMainExecutor();
        this.ioExecutor = createIOExecutor();
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        
        // 初始化组件
        this.loadBalancer = new LoadBalancer(config);
        this.resourceMonitor = new ResourceMonitor();
        
        // 启动监控任务
        startMonitoringTasks();
        
        logger.info("Reasoning task scheduler initialized with {} main threads and {} IO threads",
                   config.getThreadPoolSize(), config.getThreadPoolSize() / 2);
    }
    
    /**
     * 提交推理任务
     */
    public <T> CompletableFuture<T> submitTask(ReasoningTask<T> task) {
        String taskId = generateTaskId();
        task.setTaskId(taskId);
        task.setSubmitTime(System.currentTimeMillis());
        
        logger.debug("Submitting task: {} (type: {})", taskId, task.getType());
        
        // 检查系统负载
        if (shouldQueueTask()) {
            return queueTask(task);
        } else {
            return executeTask(task);
        }
    }
    
    /**
     * 提交批量任务
     */
    public <T> CompletableFuture<List<T>> submitBatchTasks(List<ReasoningTask<T>> tasks) {
        logger.debug("Submitting batch of {} tasks", tasks.size());
        
        List<CompletableFuture<T>> futures = new ArrayList<>();
        
        for (ReasoningTask<T> task : tasks) {
            futures.add(submitTask(task));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
    
    /**
     * 提交依赖任务
     */
    public <T> CompletableFuture<T> submitDependentTask(ReasoningTask<T> task, 
                                                       List<CompletableFuture<?>> dependencies) {
        logger.debug("Submitting dependent task: {} with {} dependencies", 
                    task.getType(), dependencies.size());
        
        // 等待所有依赖完成
        CompletableFuture<Void> allDependencies = CompletableFuture.allOf(
            dependencies.toArray(new CompletableFuture[0]));
        
        return allDependencies.thenCompose(v -> submitTask(task));
    }
    
    /**
     * 执行任务
     */
    private <T> CompletableFuture<T> executeTask(ReasoningTask<T> task) {
        activeTasks.put(task.getTaskId(), task);
        
        // 选择合适的执行器
        ExecutorService executor = selectExecutor(task);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            task.setStartTime(startTime);
            
            try {
                logger.debug("Executing task: {} (type: {})", task.getTaskId(), task.getType());
                
                T result = task.execute();
                
                long executionTime = System.currentTimeMillis() - startTime;
                recordTaskCompletion(task, executionTime, true);
                
                logger.debug("Task completed: {} in {}ms", task.getTaskId(), executionTime);
                return result;
                
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                recordTaskCompletion(task, executionTime, false);
                
                logger.error("Task failed: {} after {}ms", task.getTaskId(), executionTime, e);
                throw new RuntimeException("Task execution failed", e);
                
            } finally {
                activeTasks.remove(task.getTaskId());
                task.setEndTime(System.currentTimeMillis());
            }
        }, executor);
        
        // 设置超时
        if (task.getTimeoutMs() > 0) {
            future = future.orTimeout(task.getTimeoutMs(), TimeUnit.MILLISECONDS);
        }
        
        return future;
    }
    
    /**
     * 队列任务
     */
    private <T> CompletableFuture<T> queueTask(ReasoningTask<T> task) {
        logger.debug("Queueing task: {} due to high load", task.getTaskId());
        
        pendingTasks.offer(task);
        
        // 返回一个将来会被执行的Future
        return CompletableFuture.supplyAsync(() -> {
            // 等待任务被调度
            while (pendingTasks.contains(task)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Task interrupted while waiting", e);
                }
            }
            
            // 任务已被调度，等待结果
            try {
                return executeTask(task).get();
            } catch (Exception e) {
                throw new RuntimeException("Queued task execution failed", e);
            }
        }, scheduledExecutor);
    }
    
    /**
     * 选择执行器
     */
    private ExecutorService selectExecutor(ReasoningTask<?> task) {
        return switch (task.getType()) {
            case DATABASE_QUERY, EMBEDDING_CALCULATION -> ioExecutor;
            case GRAPH_TRAVERSAL, PATH_SCORING, RESULT_AGGREGATION -> mainExecutor;
            default -> mainExecutor;
        };
    }
    
    /**
     * 检查是否应该队列任务
     */
    private boolean shouldQueueTask() {
        int activeTaskCount = activeTasks.size();
        int maxConcurrentTasks = config.getThreadPoolSize() * 2;
        
        // 基于活跃任务数和系统资源决定
        return activeTaskCount >= maxConcurrentTasks || 
               resourceMonitor.isHighLoad();
    }
    
    /**
     * 创建主执行器
     */
    private ExecutorService createMainExecutor() {
        int corePoolSize = config.getThreadPoolSize();
        int maximumPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "reasoning-main-" + counter.incrementAndGet());
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
    
    /**
     * 创建IO执行器
     */
    private ExecutorService createIOExecutor() {
        int corePoolSize = Math.max(2, config.getThreadPoolSize() / 2);
        
        return Executors.newFixedThreadPool(corePoolSize, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "reasoning-io-" + counter.incrementAndGet());
                t.setDaemon(false);
                return t;
            }
        });
    }
    
    /**
     * 启动监控任务
     */
    private void startMonitoringTasks() {
        // 任务队列处理器
        scheduledExecutor.scheduleWithFixedDelay(this::processQueuedTasks, 1, 1, TimeUnit.SECONDS);
        
        // 性能监控
        scheduledExecutor.scheduleWithFixedDelay(this::logPerformanceMetrics, 30, 30, TimeUnit.SECONDS);
        
        // 资源监控
        scheduledExecutor.scheduleWithFixedDelay(resourceMonitor::updateMetrics, 5, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 处理队列中的任务
     */
    private void processQueuedTasks() {
        while (!pendingTasks.isEmpty() && !shouldQueueTask()) {
            ReasoningTask<?> task = pendingTasks.poll();
            if (task != null) {
                logger.debug("Processing queued task: {}", task.getTaskId());
                executeTask(task);
            }
        }
    }
    
    /**
     * 记录任务完成情况
     */
    private void recordTaskCompletion(ReasoningTask<?> task, long executionTime, boolean success) {
        totalTasksExecuted.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        
        TaskMetrics metrics = taskMetrics.computeIfAbsent(task.getType(), k -> new TaskMetrics());
        metrics.recordExecution(executionTime, success);
    }
    
    /**
     * 记录性能指标
     */
    private void logPerformanceMetrics() {
        long totalTasks = totalTasksExecuted.get();
        long totalTime = totalExecutionTime.get();
        
        if (totalTasks > 0) {
            double avgExecutionTime = (double) totalTime / totalTasks;
            int activeTaskCount = activeTasks.size();
            int queuedTasks = pendingTasks.size();
            
            logger.info("Performance Metrics - Total: {}, Avg Time: {:.2f}ms, Active: {}, Queued: {}",
                       totalTasks, avgExecutionTime, activeTaskCount, queuedTasks);
            
            // 记录各类型任务的指标
            taskMetrics.forEach((type, metrics) -> {
                logger.debug("Task Type: {} - Count: {}, Avg Time: {:.2f}ms, Success Rate: {:.1f}%",
                           type, metrics.getExecutionCount(), metrics.getAverageExecutionTime(),
                           metrics.getSuccessRate() * 100);
            });
        }
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task-" + taskIdCounter.incrementAndGet() + "-" + System.currentTimeMillis();
    }
    
    /**
     * 获取调度器统计信息
     */
    public SchedulerStatistics getStatistics() {
        return new SchedulerStatistics(
            totalTasksExecuted.get(),
            totalExecutionTime.get(),
            activeTasks.size(),
            pendingTasks.size(),
            new HashMap<>(taskMetrics),
            resourceMonitor.getCurrentMetrics()
        );
    }
    
    /**
     * 关闭调度器
     */
    public void shutdown() {
        logger.info("Shutting down reasoning task scheduler...");
        
        scheduledExecutor.shutdown();
        mainExecutor.shutdown();
        ioExecutor.shutdown();
        
        try {
            if (!mainExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                mainExecutor.shutdownNow();
            }
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            mainExecutor.shutdownNow();
            ioExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Reasoning task scheduler shutdown completed");
    }
    
    /**
     * 任务类型枚举
     */
    public enum TaskType {
        ENTITY_IDENTIFICATION,
        DATABASE_QUERY,
        EMBEDDING_CALCULATION,
        GRAPH_TRAVERSAL,
        PATH_SCORING,
        RESULT_AGGREGATION,
        LLM_GENERATION
    }
}