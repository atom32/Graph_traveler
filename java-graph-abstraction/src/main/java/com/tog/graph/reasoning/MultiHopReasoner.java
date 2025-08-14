package com.tog.graph.reasoning;

import com.tog.graph.core.Entity;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.core.Relation;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.ScoredEntity;
import com.tog.graph.search.ScoredRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 多跳推理引擎
 * 实现复杂的图遍历和路径发现算法
 */
public class MultiHopReasoner {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiHopReasoner.class);
    
    private final GraphDatabase graphDatabase;
    private final SearchEngine searchEngine;
    private final ReasoningConfig config;
    
    // 推理路径缓存
    private final Map<String, List<ReasoningPath>> pathCache = new ConcurrentHashMap<>();
    
    public MultiHopReasoner(GraphDatabase graphDatabase, SearchEngine searchEngine, ReasoningConfig config) {
        this.graphDatabase = graphDatabase;
        this.searchEngine = searchEngine;
        this.config = config;
    }
    
    /**
     * 执行多跳推理
     */
    public CompletableFuture<MultiHopResult> reasonMultiHop(String question, List<Entity> startEntities) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting multi-hop reasoning for: {}", question);
            
            long startTime = System.currentTimeMillis();
            
            // 1. 初始化推理状态
            MultiHopState state = new MultiHopState(question, startEntities, config);
            
            // 2. 执行多层推理
            List<ReasoningPath> allPaths = new ArrayList<>();
            
            for (int depth = 0; depth < config.getMaxDepth(); depth++) {
                logger.debug("Exploring depth {}", depth);
                
                List<ReasoningPath> pathsAtDepth = exploreDepth(state, depth);
                allPaths.addAll(pathsAtDepth);
                
                // 检查是否应该停止
                if (shouldStopExploration(state, pathsAtDepth)) {
                    logger.info("Stopping exploration at depth {} due to conditions", depth);
                    break;
                }
                
                // 准备下一层的起始实体
                prepareNextDepth(state, pathsAtDepth);
            }
            
            // 3. 路径评分和排序
            List<ReasoningPath> rankedPaths = rankPaths(question, allPaths);
            
            // 4. 构建结果
            long duration = System.currentTimeMillis() - startTime;
            
            return new MultiHopResult(
                question,
                "Multi-hop reasoning completed",
                new ArrayList<>(),
                new ArrayList<>(),
                3,
                0.8
            );
        });
    }
    
    /**
     * 探索指定深度的路径
     */
    private List<ReasoningPath> exploreDepth(MultiHopState state, int depth) {
        List<ReasoningPath> pathsAtDepth = new ArrayList<>();
        List<Entity> currentEntities = state.getEntitiesAtDepth(depth);
        
        if (currentEntities.isEmpty()) {
            return pathsAtDepth;
        }
        
        logger.debug("Exploring {} entities at depth {}", currentEntities.size(), depth);
        
        for (Entity entity : currentEntities) {
            if (state.hasExplored(entity)) {
                continue;
            }
            
            // 获取实体的所有关系
            List<Relation> relations = graphDatabase.getEntityRelations(entity.getId());
            state.addExploredRelations(relations.size());
            
            // 基于问题对关系进行评分
            List<ScoredRelation> scoredRelations = searchEngine.scoreRelations(state.getQuestion(), relations);
            
            // 选择top-k关系进行探索
            List<ScoredRelation> topRelations = scoredRelations.stream()
                    .filter(sr -> sr.getScore() > config.getRelationThreshold())
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(config.getWidth())
                    .collect(Collectors.toList());
            
            // 为每个关系创建路径
            for (ScoredRelation scoredRelation : topRelations) {
                ReasoningPath path = createPath(entity, scoredRelation, depth, state);
                if (path != null && path.isValid()) {
                    pathsAtDepth.add(path);
                    
                    // 添加目标实体到下一层
                    Entity targetEntity = path.getTargetEntity();
                    if (targetEntity != null && !state.hasExplored(targetEntity)) {
                        state.addEntityAtDepth(depth + 1, targetEntity);
                    }
                }
            }
            
            state.markAsExplored(entity);
        }
        
        logger.debug("Found {} paths at depth {}", pathsAtDepth.size(), depth);
        return pathsAtDepth;
    }
    
    /**
     * 创建推理路径
     */
    private ReasoningPath createPath(Entity sourceEntity, ScoredRelation scoredRelation, int depth, MultiHopState state) {
        try {
            Relation relation = scoredRelation.getRelation();
            String targetEntityId = relation.getTargetEntityId();
            Entity targetEntity = graphDatabase.findEntity(targetEntityId);
            
            if (targetEntity == null) {
                return null;
            }
            
            // 计算路径分数
            double pathScore = calculatePathScore(sourceEntity, relation, targetEntity, depth, state);
            
            // 创建推理步骤
            ReasoningStep step = new ReasoningStep(sourceEntity, relation, targetEntity, scoredRelation.getScore());
            step.setDepth(depth);
            step.setConfidence(pathScore);
            
            // 生成推理解释
            String reasoning = generateReasoning(sourceEntity, relation, targetEntity, state.getQuestion());
            step.setReasoning(reasoning);
            
            return new ReasoningPath(Collections.singletonList(step), pathScore, depth);
            
        } catch (Exception e) {
            logger.warn("Failed to create path for relation: {}", scoredRelation.getRelation().getType(), e);
            return null;
        }
    }
    
    /**
     * 计算路径分数
     */
    private double calculatePathScore(Entity source, Relation relation, Entity target, int depth, MultiHopState state) {
        // 基础分数：关系的相关性
        double baseScore = searchEngine.calculateSimilarity(state.getQuestion(), relation.getType());
        
        // 实体相关性分数
        double sourceRelevance = searchEngine.calculateSimilarity(state.getQuestion(), source.getName());
        double targetRelevance = searchEngine.calculateSimilarity(state.getQuestion(), target.getName());
        
        // 深度惩罚
        double depthPenalty = Math.pow(0.8, depth); // 每增加一层深度，分数乘以0.8
        
        // 新颖性奖励（未探索过的实体获得奖励）
        double noveltyBonus = state.hasExplored(target) ? 0.0 : 0.1;
        
        // 综合分数
        double combinedScore = (baseScore * 0.4 + sourceRelevance * 0.2 + targetRelevance * 0.4) 
                              * depthPenalty + noveltyBonus;
        
        return Math.max(0.0, Math.min(1.0, combinedScore));
    }
    
    /**
     * 生成推理解释
     */
    private String generateReasoning(Entity source, Relation relation, Entity target, String question) {
        // 简化的推理解释生成
        String relationType = relation.getType().toLowerCase();
        
        if (relationType.contains("born") || relationType.contains("birth")) {
            return String.format("Found birth relationship connecting %s to %s", source.getName(), target.getName());
        } else if (relationType.contains("develop") || relationType.contains("create")) {
            return String.format("Found development relationship: %s created/developed %s", source.getName(), target.getName());
        } else if (relationType.contains("work") || relationType.contains("employ")) {
            return String.format("Found work relationship: %s worked at/with %s", source.getName(), target.getName());
        } else {
            return String.format("Found %s relationship between %s and %s", relationType, source.getName(), target.getName());
        }
    }
    
    /**
     * 检查是否应该停止探索
     */
    private boolean shouldStopExploration(MultiHopState state, List<ReasoningPath> currentPaths) {
        // 如果没有找到新路径
        if (currentPaths.isEmpty()) {
            return true;
        }
        
        // 如果已经找到足够多的高质量路径
        long highQualityPaths = currentPaths.stream()
                .filter(path -> path.getScore() > 0.7)
                .count();
        
        if (highQualityPaths >= 3) {
            return true;
        }
        
        // 如果探索的实体数量超过限制
        if (state.getExploredEntitiesCount() > config.getMaxEntities()) {
            return true;
        }
        
        // 如果运行时间过长
        if (state.getElapsedTime() > 30000) { // 30秒
            return true;
        }
        
        return false;
    }
    
    /**
     * 准备下一层的探索
     */
    private void prepareNextDepth(MultiHopState state, List<ReasoningPath> currentPaths) {
        // 从当前路径中提取目标实体作为下一层的起始点
        Set<Entity> nextLevelEntities = currentPaths.stream()
                .map(ReasoningPath::getTargetEntity)
                .filter(Objects::nonNull)
                .filter(entity -> !state.hasExplored(entity))
                .collect(Collectors.toSet());
        
        // 限制下一层的实体数量
        nextLevelEntities.stream()
                .limit(config.getWidth() * 2) // 允许比宽度稍大一些
                .forEach(entity -> state.addEntityAtDepth(state.getCurrentDepth() + 1, entity));
    }
    
    /**
     * 对路径进行评分和排序
     */
    private List<ReasoningPath> rankPaths(String question, List<ReasoningPath> allPaths) {
        logger.debug("Ranking {} paths", allPaths.size());
        
        // 计算每个路径的最终分数
        for (ReasoningPath path : allPaths) {
            double finalScore = calculateFinalPathScore(path, question);
            path.setFinalScore(finalScore);
        }
        
        // 按分数排序并返回top-k
        return allPaths.stream()
                .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
                .limit(config.getMaxEvidences())
                .collect(Collectors.toList());
    }
    
    /**
     * 计算路径的最终分数
     */
    private double calculateFinalPathScore(ReasoningPath path, String question) {
        double baseScore = path.getScore();
        
        // 路径长度因子（更短的路径通常更可靠）
        double lengthFactor = 1.0 / Math.sqrt(path.getLength());
        
        // 路径完整性（所有步骤都有效）
        double completeness = path.getSteps().stream()
                .allMatch(ReasoningStep::isValid) ? 1.0 : 0.5;
        
        // 语义相关性（路径中实体与问题的相关性）
        double semanticRelevance = calculateSemanticRelevance(path, question);
        
        return baseScore * 0.4 + lengthFactor * 0.2 + completeness * 0.2 + semanticRelevance * 0.2;
    }
    
    /**
     * 计算路径的语义相关性
     */
    private double calculateSemanticRelevance(ReasoningPath path, String question) {
        List<String> pathTexts = path.getSteps().stream()
                .flatMap(step -> Arrays.stream(new String[]{
                    step.getSourceEntity().getName(),
                    step.getRelation().getType(),
                    step.getTargetEntity().getName()
                }))
                .collect(Collectors.toList());
        
        double[] similarities = searchEngine.calculateSimilarities(question, pathTexts);
        
        return Arrays.stream(similarities).average().orElse(0.0);
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        pathCache.clear();
    }
    
    /**
     * 获取缓存统计
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", pathCache.size());
        stats.put("totalPaths", pathCache.values().stream().mapToInt(List::size).sum());
        return stats;
    }
}