package com.tog.graph.reasoning.parallel.tasks;

import com.tog.graph.core.Entity;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.core.Relation;
import com.tog.graph.reasoning.parallel.ReasoningTask;
import com.tog.graph.reasoning.parallel.ReasoningTaskScheduler;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.ScoredRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图遍历任务
 * 从给定实体开始遍历图结构
 */
public class GraphTraversalTask extends ReasoningTask<List<com.tog.graph.reasoning.parallel.tasks.TraversalResult>> {
    
    private final GraphDatabase graphDatabase;
    private final SearchEngine searchEngine;
    private final List<Entity> startEntities;
    private final String question;
    private final int maxDepth;
    private final int maxWidth;
    private final double relationThreshold;
    
    public GraphTraversalTask(GraphDatabase graphDatabase, SearchEngine searchEngine,
                             List<Entity> startEntities, String question,
                             int maxDepth, int maxWidth, double relationThreshold) {
        super(ReasoningTaskScheduler.TaskType.GRAPH_TRAVERSAL, 
              "Traverse graph from " + startEntities.size() + " entities");
        
        this.graphDatabase = graphDatabase;
        this.searchEngine = searchEngine;
        this.startEntities = startEntities;
        this.question = question;
        this.maxDepth = maxDepth;
        this.maxWidth = maxWidth;
        this.relationThreshold = relationThreshold;
        
        // 设置上下文参数
        setContextParameter("startEntities", startEntities.size());
        setContextParameter("maxDepth", maxDepth);
        setContextParameter("maxWidth", maxWidth);
        setContextParameter("relationThreshold", relationThreshold);
    }
    
    @Override
    public List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> execute() throws Exception {
        beforeExecute();
        
        try {
            List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> allResults = new ArrayList<>();
            int totalRelationsExplored = 0;
            
            for (Entity startEntity : startEntities) {
                List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> entityResults = traverseFromEntity(startEntity);
                allResults.addAll(entityResults);
                
                // 统计探索的关系数
                totalRelationsExplored += entityResults.stream()
                        .mapToInt(result -> result.getRelations().size())
                        .sum();
            }
            
            // 记录统计信息
            setContextParameter("totalResults", allResults.size());
            setContextParameter("totalRelationsExplored", totalRelationsExplored);
            setContextParameter("avgResultsPerEntity", 
                              startEntities.isEmpty() ? 0.0 : (double) allResults.size() / startEntities.size());
            
            afterExecute(true);
            return allResults;
            
        } catch (Exception e) {
            setErrorMessage("Graph traversal failed: " + e.getMessage());
            afterExecute(false);
            throw e;
        }
    }
    
    /**
     * 从单个实体开始遍历
     */
    private List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> traverseFromEntity(Entity startEntity) {
        List<com.tog.graph.reasoning.parallel.tasks.TraversalResult> results = new ArrayList<>();
        
        try {
            // 获取实体的所有关系
            List<Relation> relations = graphDatabase.getEntityRelations(startEntity.getId());
            
            if (relations.isEmpty()) {
                return results;
            }
            
            // 基于问题对关系进行评分
            List<ScoredRelation> scoredRelations = searchEngine.scoreRelations(question, relations);
            
            // 选择top-k关系
            List<ScoredRelation> topRelations = scoredRelations.stream()
                    .filter(sr -> sr.getScore() > relationThreshold)
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(maxWidth)
                    .collect(Collectors.toList());
            
            // 为每个关系创建遍历结果
            for (ScoredRelation scoredRelation : topRelations) {
                Relation relation = scoredRelation.getRelation();
                
                try {
                    // 获取目标实体
                    Entity targetEntity = graphDatabase.findEntity(relation.getTargetEntityId());
                    
                    if (targetEntity != null) {
                        com.tog.graph.reasoning.parallel.tasks.TraversalResult result = 
                            new com.tog.graph.reasoning.parallel.tasks.TraversalResult(
                                List.of(startEntity, targetEntity),
                                List.of(relation)
                            );
                        
                        results.add(result);
                    }
                    
                } catch (Exception e) {
                    // 记录但不中断处理
                    setContextParameter("traversalErrors", 
                                      getContextParameter("traversalErrors", 0) + 1);
                }
            }
            
        } catch (Exception e) {
            // 记录实体级别的错误
            setContextParameter("entityErrors", 
                              getContextParameter("entityErrors", 0) + 1);
        }
        
        return results;
    }
    
}