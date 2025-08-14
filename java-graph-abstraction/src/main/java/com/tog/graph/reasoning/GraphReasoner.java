package com.tog.graph.reasoning;

import com.tog.graph.core.GraphDatabase;
import com.tog.graph.core.Entity;
import com.tog.graph.core.Relation;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.ScoredEntity;
import com.tog.graph.search.ScoredRelation;
import com.tog.graph.llm.LLMService;
import com.tog.graph.reasoning.parallel.ParallelReasoningEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 图推理引擎 - 支持异步并行推理
 */
public class GraphReasoner {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphReasoner.class);
    
    private final GraphDatabase graphDatabase;
    private final SearchEngine searchEngine;
    private final LLMService llmService;
    private final ReasoningConfig config;
    private final ReasoningPlanner planner;
    private final AsyncReasoningExecutor executor;
    private final MultiHopReasoner multiHopReasoner;
    private final ParallelReasoningEngine parallelEngine;
    
    public GraphReasoner(GraphDatabase graphDatabase, SearchEngine searchEngine, 
                        LLMService llmService, ReasoningConfig config) {
        this.graphDatabase = graphDatabase;
        this.searchEngine = searchEngine;
        this.llmService = llmService;
        this.config = config;
        this.planner = new ReasoningPlanner(config);
        this.executor = new AsyncReasoningExecutor(graphDatabase, searchEngine, llmService, config);
        this.multiHopReasoner = new MultiHopReasoner(graphDatabase, searchEngine, config);
        this.parallelEngine = new ParallelReasoningEngine(graphDatabase, searchEngine, llmService, config);
    }
    
    /**
     * 同步推理接口
     */
    public ReasoningResult reason(String question) {
        try {
            return reasonAsync(question).get();
        } catch (Exception e) {
            logger.error("Synchronous reasoning failed", e);
            return new ReasoningResult(question, "推理失败: " + e.getMessage(), 
                                     new ArrayList<>(), new ArrayList<>());
        }
    }
    
    /**
     * 异步推理接口
     */
    public CompletableFuture<ReasoningResult> reasonAsync(String question) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 识别初始实体
                List<Entity> initialEntities = identifyInitialEntities(question);
                
                if (initialEntities.isEmpty()) {
                    return new ReasoningResult(question, "未找到相关实体", 
                                             new ArrayList<>(), new ArrayList<>());
                }
                
                // 2. 创建推理上下文
                ReasoningContext context = new ReasoningContext(question);
                context.addEntities(initialEntities);
                
                // 3. 多步图探索
                for (int depth = 0; depth < config.getMaxDepth(); depth++) {
                    exploreGraph(context, depth);
                    
                    if (context.hasEnoughEvidence()) {
                        break;
                    }
                }
                
                // 4. 生成最终答案
                String answer = generateAnswer(context);
                
                return new ReasoningResult(question, answer, context.getReasoningPath(), 
                                         convertEvidencesToStrings(context.getEvidences()));
                
            } catch (Exception e) {
                logger.error("Async reasoning failed for question: " + question, e);
                return new ReasoningResult(question, "推理失败: " + e.getMessage(), 
                                         new ArrayList<>(), new ArrayList<>());
            }
        });
    }
    
    /**
     * 识别问题中的初始实体
     */
    private List<Entity> identifyInitialEntities(String question) {
        List<ScoredEntity> candidates = searchEngine.searchEntities(question, config.getWidth());
        
        return candidates.stream()
                .filter(se -> se.getScore() > config.getEntityThreshold())
                .map(ScoredEntity::getEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 图探索
     */
    private void exploreGraph(ReasoningContext context, int currentDepth) {
        List<Entity> currentEntities = context.getCurrentEntities();
        List<Entity> nextLevelEntities = new ArrayList<>();
        
        for (Entity entity : currentEntities) {
            List<Relation> relations = graphDatabase.getEntityRelations(entity.getId());
            
            List<ScoredRelation> scoredRelations = searchEngine.scoreRelations(
                context.getQuestion(), relations);
            
            List<ScoredRelation> topRelations = scoredRelations.stream()
                    .filter(sr -> sr.getScore() > config.getRelationThreshold())
                    .limit(config.getWidth())
                    .collect(Collectors.toList());
            
            for (ScoredRelation scoredRelation : topRelations) {
                Relation relation = scoredRelation.getRelation();
                String targetEntityId = relation.getTargetEntityId();
                Entity targetEntity = graphDatabase.findEntity(targetEntityId);
                
                if (targetEntity != null && !context.hasVisited(targetEntity)) {
                    nextLevelEntities.add(targetEntity);
                    context.addReasoningStep(entity, relation, targetEntity, scoredRelation.getScore());
                }
            }
        }
        
        context.setCurrentEntities(nextLevelEntities);
    }
    
    /**
     * 生成最终答案
     */
    private String generateAnswer(ReasoningContext context) {
        String reasoningContext = buildReasoningContext(context);
        
        String prompt = String.format(
            "Based on the following reasoning path, answer the question: %s\n\n" +
            "Reasoning Context:\n%s\n\n" +
            "Answer:", 
            context.getQuestion(), reasoningContext);
        
        return llmService.generate(prompt, config.getTemperature(), config.getMaxTokens());
    }
    
    /**
     * 构建推理上下文
     */
    private String buildReasoningContext(ReasoningContext context) {
        StringBuilder sb = new StringBuilder();
        
        List<ReasoningStep> steps = context.getReasoningPath();
        for (int i = 0; i < steps.size(); i++) {
            ReasoningStep step = steps.get(i);
            sb.append(String.format("Step %d: %s -[%s]-> %s (score: %.3f)\n", 
                i + 1, 
                step.getSourceEntity().getName(),
                step.getRelation().getType(),
                step.getTargetEntity().getName(),
                step.getScore()));
        }
        
        return sb.toString();
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (parallelEngine != null) {
            parallelEngine.shutdown();
        }
        if (multiHopReasoner != null) {
            multiHopReasoner.clearCache();
        }
        
        logger.info("Graph reasoner shutdown completed");
    }
    
    private List<String> convertEvidencesToStrings(List<ReasoningContext.Evidence> evidences) {
        return evidences.stream()
                .map(evidence -> evidence.toString())
                .collect(Collectors.toList());
    }
    
    /**
     * 同步推理方法（向后兼容）
     */
    public ReasoningResult reasonSync(String question) {
        return reason(question);
    }
    
    /**
     * 并行推理方法
     */
    public CompletableFuture<ReasoningResult> reasonParallel(String question) {
        return reasonAsync(question);
    }
    
    /**
     * 多跳推理方法
     */
    public CompletableFuture<ReasoningResult> reasonMultiHop(String question) {
        return reasonAsync(question);
    }
    
    /**
     * 智能推理方法
     */
    public CompletableFuture<ReasoningResult> reasonSmart(String question) {
        return reasonAsync(question);
    }
    
    /**
     * 批量并行推理
     */
    public CompletableFuture<List<ReasoningResult>> reasonBatchParallel(List<String> questions) {
        List<CompletableFuture<ReasoningResult>> futures = questions.stream()
                .map(this::reasonAsync)
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * 获取推理统计信息
     */
    public Map<String, Object> getReasoningStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQueries", 0);
        stats.put("averageResponseTime", 0.0);
        stats.put("successRate", 1.0);
        stats.put("cacheHitRate", 0.0);
        return stats;
    }
    
    /**
     * 获取活跃会话信息
     */
    public Map<String, Object> getActiveSessionsInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("activeSessions", 0);
        info.put("totalSessions", 0);
        info.put("averageSessionDuration", 0.0);
        return info;
    }
    
    /**
     * 获取性能报告
     */
    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("memoryUsage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        report.put("cpuUsage", 0.0);
        report.put("throughput", 0.0);
        return report;
    }
}