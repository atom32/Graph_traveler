package com.tog.graph.reasoning.parallel.tasks;

import com.tog.graph.core.Entity;
import com.tog.graph.reasoning.parallel.ReasoningTask;
import com.tog.graph.reasoning.parallel.ReasoningTaskScheduler;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.ScoredEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 实体识别任务
 * 从问题中识别相关实体
 */
public class EntityIdentificationTask extends ReasoningTask<List<Entity>> {
    
    private final SearchEngine searchEngine;
    private final String question;
    private final int maxEntities;
    private final double threshold;
    
    public EntityIdentificationTask(SearchEngine searchEngine, String question, 
                                   int maxEntities, double threshold) {
        super(ReasoningTaskScheduler.TaskType.ENTITY_IDENTIFICATION, 
              "Identify entities from question: " + question.substring(0, Math.min(50, question.length())));
        
        this.searchEngine = searchEngine;
        this.question = question;
        this.maxEntities = maxEntities;
        this.threshold = threshold;
        
        // 设置上下文参数
        setContextParameter("question", question);
        setContextParameter("maxEntities", maxEntities);
        setContextParameter("threshold", threshold);
    }
    
    @Override
    public List<Entity> execute() throws Exception {
        beforeExecute();
        
        try {
            // 使用搜索引擎识别实体
            List<ScoredEntity> scoredEntities = searchEngine.searchEntities(question, maxEntities * 2);
            
            // 过滤和转换结果
            List<Entity> entities = scoredEntities.stream()
                    .filter(se -> se.getScore() > threshold)
                    .limit(maxEntities)
                    .map(ScoredEntity::getEntity)
                    .collect(Collectors.toList());
            
            // 记录结果到上下文
            setContextParameter("foundEntities", entities.size());
            setContextParameter("avgScore", scoredEntities.stream()
                    .mapToDouble(ScoredEntity::getScore)
                    .average()
                    .orElse(0.0));
            
            afterExecute(true);
            return entities;
            
        } catch (Exception e) {
            setErrorMessage("Entity identification failed: " + e.getMessage());
            afterExecute(false);
            throw e;
        }
    }
}