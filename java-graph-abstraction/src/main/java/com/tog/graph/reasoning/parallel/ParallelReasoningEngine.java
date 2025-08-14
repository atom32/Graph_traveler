package com.tog.graph.reasoning.parallel;

import com.tog.graph.core.Entity;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.reasoning.MultiHopResult;
import com.tog.graph.reasoning.ReasoningConfig;
import com.tog.graph.reasoning.ReasoningResult;
import com.tog.graph.reasoning.parallel.tasks.EntityIdentificationTask;
import com.tog.graph.reasoning.parallel.tasks.GraphTraversalTask;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 并行推理引擎
 * 整合任务调度、负载均衡和资源监控，实现高性能的并行推理
 */
public class ParallelReasoningEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ParallelReasoningEngine.class);
    
    private final GraphDatabase graphDatabase;
    private final SearchEngine searchEngine;
    private final LLMService llmService;
    private final ReasoningConfig config;
    
    private final ReasoningTaskScheduler taskScheduler;
    private final Map<String, CompletableFuture<ReasoningResult>> activeReasoningSessions;
    private final java.util.concurrent.ExecutorService executorService;
    
    // 性能统计
    private long totalQuestionsProcessed = 0;
    private long totalProcessingTime = 0;
    
    public ParallelReasoningEngine(GraphDatabase graphDatabase, SearchEngine searchEngine,
                                  LLMService llmService, ReasoningConfig config) {
        this.graphDatabase = graphDatabase;
        this.searchEngine = searchEngine;
        this.llmService = llmService;
        this.config = config;
        
        this.taskScheduler = new ReasoningTaskScheduler(config);
        this.activeReasoningSessions = new HashMap<>();
        this.executorService = java.util.concurrent.Executors.newFixedThreadPool(config.getThreadPoolSize());
        
        logger.info("Parallel reasoning engine initialized with {} threads", 
                   config.getThreadPoolSize());
    }
    
    /**
     * 并行推理主接口
     */
    public CompletableFuture<ReasoningResult> reasonParallel(String question) {
        String sessionId = generateSessionId();
        logger.info("Starting parallel reasoning session: {} for question: {}", sessionId, question);
        
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<ReasoningResult> reasoningFuture = executeParallelReasoning(question, sessionId)
                .whenComplete((result, throwable) -> {
                    long duration = System.currentTimeMillis() - startTime;
                    
                    synchronized (this) {
                        totalQuestionsProcessed++;
                        totalProcessingTime += duration;
                    }
                    
                    activeReasoningSessions.remove(sessionId);
                    
                    if (throwable != null) {
                        logger.error("Parallel reasoning session {} failed after {}ms", 
                                   sessionId, duration, throwable);
                    } else {
                        logger.info("Parallel reasoning session {} completed in {}ms", 
                                  sessionId, duration);
                    }
                });
        
        activeReasoningSessions.put(sessionId, reasoningFuture);
        return reasoningFuture;
    }
    
    /**
     * 批量并行推理
     */
    public CompletableFuture<List<ReasoningResult>> reasonBatch(List<String> questions) {
        logger.info("Starting batch parallel reasoning for {} questions", questions.size());
        
        List<CompletableFuture<ReasoningResult>> futures = questions.stream()
                .map(this::reasonParallel)
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * 执行并行推理的核心逻辑
     */
    private CompletableFuture<ReasoningResult> executeParallelReasoning(String question, String sessionId) {
        // 第一阶段：实体识别（并行执行多个策略）
        CompletableFuture<List<Entity>> entityIdentificationFuture = executeEntityIdentification(question);
        
        // 第二阶段：基于识别的实体进行图遍历（依赖第一阶段）
        CompletableFuture<List<com.tog.graph.reasoning.parallel.tasks.TraversalResult>> traversalFuture = 
                entityIdentificationFuture.thenCompose(entities -> executeGraphTraversal(question, entities));
        
        // 第三阶段：路径评分和结果聚合（依赖第二阶段）
        CompletableFuture<List<String>> evidenceFuture = 
                traversalFuture.thenCompose(results -> executeEvidenceCollection(results));
        
        // 第四阶段：答案生成（依赖第三阶段）
        CompletableFuture<String> answerFuture = 
                evidenceFuture.thenCompose(evidences -> executeAnswerGeneration(question, evidences));
        
        // 最终阶段：构建推理结果
        return CompletableFuture.allOf(entityIdentificationFuture, traversalFuture, evidenceFuture, answerFuture)
                .thenApply(v -> {
                    try {
                        List<Entity> entities = entityIdentificationFuture.get();
                        List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> traversalResults = traversalFuture.get();
                        List<String> evidences = evidenceFuture.get();
                        String answer = answerFuture.get();
                        
                        return buildReasoningResult(question, entities, traversalResults, evidences, answer);
                        
                    } catch (Exception e) {
                        logger.error("Failed to build reasoning result for session: {}", sessionId, e);
                        throw new RuntimeException("Failed to build reasoning result", e);
                    }
                });
    }
    
    /**
     * 执行实体识别阶段
     */
    private CompletableFuture<List<Entity>> executeEntityIdentification(String question) {
        // 创建多个并行的实体识别任务
        List<ReasoningTask<List<Entity>>> identificationTasks = Arrays.asList(
            // 主要实体识别
            new EntityIdentificationTask(searchEngine, question, config.getWidth(), config.getEntityThreshold()),
            
            // 宽松阈值的实体识别（作为补充）
            new EntityIdentificationTask(searchEngine, question, config.getWidth() * 2, config.getEntityThreshold() * 0.7)
        );
        
        // 并行执行所有识别任务
        return taskScheduler.submitBatchTasks(identificationTasks)
                .thenApply(this::mergeEntityLists);
    }
    
    /**
     * 执行图遍历阶段
     */
    private CompletableFuture<List<com.tog.graph.reasoning.parallel.tasks.TraversalResult>> executeGraphTraversal(
            String question, List<Entity> entities) {
        
        if (entities.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        // 将实体分组进行并行遍历
        int batchSize = Math.max(1, entities.size() / config.getThreadPoolSize());
        List<List<Entity>> entityBatches = partitionList(entities, batchSize);
        
        List<GraphTraversalTask> traversalTasks = entityBatches.stream()
                .map(batch -> new GraphTraversalTask(
                    graphDatabase, searchEngine, batch, question,
                    config.getMaxDepth(), config.getWidth(), config.getRelationThreshold()))
                .collect(Collectors.toList());
        
        // 使用原始类型避免泛型问题
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<Void>> taskFutures = new ArrayList<>();
            @SuppressWarnings("rawtypes")
            List allResults = Collections.synchronizedList(new ArrayList<>());
            
            for (GraphTraversalTask task : traversalTasks) {
                CompletableFuture<Void> taskFuture = CompletableFuture.runAsync(() -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List taskResults = task.execute();
                        allResults.addAll(taskResults);
                    } catch (Exception e) {
                        logger.error("Error executing traversal task", e);
                    }
                }, executorService);
                taskFutures.add(taskFuture);
            }
            
            // 等待所有任务完成
            CompletableFuture.allOf(taskFutures.toArray(new CompletableFuture[0])).join();
            
            @SuppressWarnings("unchecked")
            List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> typedResults = 
                (List<com.tog.graph.reasoning.parallel.tasks.TraversalResult>) allResults;
            return typedResults;
        });
    }
    
    /**
     * 执行证据收集阶段
     */
    private CompletableFuture<List<String>> executeEvidenceCollection(
            List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> traversalResults) {
        
        return CompletableFuture.supplyAsync(() -> {
            // 按分数排序并选择最佳证据
            return traversalResults.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(config.getMaxEvidences())
                    .map(result -> result.toString()) // 使用toString()代替getPathDescription()
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * 执行答案生成阶段
     */
    private CompletableFuture<String> executeAnswerGeneration(String question, List<String> evidences) {
        return CompletableFuture.supplyAsync(() -> {
            if (evidences.isEmpty()) {
                return "I don't have enough information to answer this question.";
            }
            
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("Based on the following evidence from a knowledge graph, answer the question:\n\n");
            prompt.append("Question: ").append(question).append("\n\n");
            prompt.append("Evidence:\n");
            
            for (int i = 0; i < evidences.size(); i++) {
                prompt.append(i + 1).append(". ").append(evidences.get(i)).append("\n");
            }
            
            prompt.append("\nAnswer:");
            
            // 调用LLM生成答案
            return llmService.generate(prompt.toString(), config.getTemperature(), config.getMaxTokens());
        });
    }
    
    /**
     * 构建推理结果
     */
    private ReasoningResult buildReasoningResult(String question, List<Entity> entities,
                                               List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> traversalResults,
                                               List<String> evidences, String answer) {
        // 简化的推理步骤构建
        List<com.tog.graph.reasoning.ReasoningStep> reasoningSteps = new ArrayList<>();
        
        // 将遍历结果转换为推理步骤
        for (com.tog.graph.reasoning.parallel.tasks.TraversalResult result : traversalResults) {
            if (!result.getRelations().isEmpty()) {
                com.tog.graph.core.Relation relation = result.getRelations().get(0);
                com.tog.graph.reasoning.ReasoningStep step = new com.tog.graph.reasoning.ReasoningStep(
                    result.getStartEntity(),
                    relation,
                    result.getEndEntity(),
                    result.getScore()
                );
                reasoningSteps.add(step);
            }
        }
        
        return new ReasoningResult(question, answer, reasoningSteps, evidences);
    }
    
    /**
     * 合并实体列表
     */
    private List<Entity> mergeEntityLists(List<List<Entity>> entityLists) {
        Set<String> seenIds = new HashSet<>();
        List<Entity> mergedEntities = new ArrayList<>();
        
        for (List<Entity> entityList : entityLists) {
            for (Entity entity : entityList) {
                if (!seenIds.contains(entity.getId())) {
                    seenIds.add(entity.getId());
                    mergedEntities.add(entity);
                }
            }
        }
        
        return mergedEntities;
    }
    
    /**
     * 合并遍历结果
     */
    private List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> mergeTraversalResults(
            List<List<com.tog.graph.reasoning.parallel.tasks.TraversalResult>> resultLists) {
        
        List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> mergedResults = new ArrayList<>();
        
        for (List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> resultList : resultLists) {
            mergedResults.addAll(resultList);
        }
        
        return mergedResults;
    }
    
    /**
     * 将列表分割为批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            partitions.add(new ArrayList<>(list.subList(i, end)));
        }
        
        return partitions;
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "session-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(new Random().nextInt());
    }
    
    /**
     * 获取引擎统计信息
     */
    public ParallelEngineStatistics getStatistics() {
        SchedulerStatistics schedulerStats = taskScheduler.getStatistics();
        
        synchronized (this) {
            return new ParallelEngineStatistics(
                totalQuestionsProcessed,
                totalProcessingTime,
                activeReasoningSessions.size(),
                schedulerStats
            );
        }
    }
    
    /**
     * 获取活跃会话信息
     */
    public Map<String, String> getActiveSessionsInfo() {
        Map<String, String> sessionInfo = new HashMap<>();
        
        activeReasoningSessions.forEach((sessionId, future) -> {
            String status = future.isDone() ? "Completed" : 
                           future.isCancelled() ? "Cancelled" : 
                           future.isCompletedExceptionally() ? "Failed" : "Running";
            sessionInfo.put(sessionId, status);
        });
        
        return sessionInfo;
    }
    
    /**
     * 取消指定会话
     */
    public boolean cancelSession(String sessionId) {
        CompletableFuture<ReasoningResult> future = activeReasoningSessions.get(sessionId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                activeReasoningSessions.remove(sessionId);
                logger.info("Cancelled reasoning session: {}", sessionId);
            }
            return cancelled;
        }
        return false;
    }
    
    /**
     * 取消所有活跃会话
     */
    public int cancelAllSessions() {
        int cancelledCount = 0;
        
        for (Map.Entry<String, CompletableFuture<ReasoningResult>> entry : activeReasoningSessions.entrySet()) {
            if (entry.getValue().cancel(true)) {
                cancelledCount++;
            }
        }
        
        activeReasoningSessions.clear();
        logger.info("Cancelled {} reasoning sessions", cancelledCount);
        
        return cancelledCount;
    }
    
    /**
     * 关闭并行推理引擎
     */
    public void shutdown() {
        logger.info("Shutting down parallel reasoning engine...");
        
        // 取消所有活跃会话
        int cancelledSessions = cancelAllSessions();
        
        // 关闭任务调度器
        taskScheduler.shutdown();
        
        // 关闭执行器服务
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Parallel reasoning engine shutdown completed. Cancelled {} sessions.", 
                   cancelledSessions);
    }
    
    /**
     * 等待所有任务完成并关闭
     */
    public void shutdownGracefully(long timeout, TimeUnit unit) {
        logger.info("Gracefully shutting down parallel reasoning engine...");
        
        try {
            // 等待活跃会话完成
            CompletableFuture<Void> allSessions = CompletableFuture.allOf(
                activeReasoningSessions.values().toArray(new CompletableFuture[0]));
            
            allSessions.get(timeout, unit);
            
        } catch (Exception e) {
            logger.warn("Some sessions did not complete within timeout, forcing shutdown", e);
            cancelAllSessions();
        }
        
        // 关闭任务调度器
        taskScheduler.shutdown();
        
        logger.info("Parallel reasoning engine graceful shutdown completed");
    }
    
    /**
     * 并行引擎统计信息
     */
    public static class ParallelEngineStatistics {
        private final long totalQuestionsProcessed;
        private final long totalProcessingTime;
        private final int activeSessionCount;
        private final SchedulerStatistics schedulerStatistics;
        
        public ParallelEngineStatistics(long totalQuestionsProcessed, long totalProcessingTime,
                                       int activeSessionCount, SchedulerStatistics schedulerStatistics) {
            this.totalQuestionsProcessed = totalQuestionsProcessed;
            this.totalProcessingTime = totalProcessingTime;
            this.activeSessionCount = activeSessionCount;
            this.schedulerStatistics = schedulerStatistics;
        }
        
        public double getAverageProcessingTime() {
            return totalQuestionsProcessed > 0 ? 
                   (double) totalProcessingTime / totalQuestionsProcessed : 0.0;
        }
        
        public double getQuestionsPerSecond() {
            double avgTime = getAverageProcessingTime();
            return avgTime > 0 ? 1000.0 / avgTime : 0.0;
        }
        
        public String getSummary() {
            return String.format(
                "ParallelEngine[questions=%d, avgTime=%.2fms, qps=%.2f, active=%d]",
                totalQuestionsProcessed, getAverageProcessingTime(), 
                getQuestionsPerSecond(), activeSessionCount);
        }
        
        // Getters
        public long getTotalQuestionsProcessed() { return totalQuestionsProcessed; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public int getActiveSessionCount() { return activeSessionCount; }
        public SchedulerStatistics getSchedulerStatistics() { return schedulerStatistics; }
        
        @Override
        public String toString() {
            return getSummary();
        }
    }
}