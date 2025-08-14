package com.tog.graph.search;

import com.tog.graph.core.Entity;
import com.tog.graph.core.Relation;

import java.util.List;

/**
 * 搜索引擎接口
 * 提供基于语义的实体和关系搜索功能
 */
public interface SearchEngine {
    
    /**
     * 初始化搜索引擎
     */
    void initialize();
    
    /**
     * 基于查询文本搜索相关实体
     */
    List<ScoredEntity> searchEntities(String query, int topK);
    
    /**
     * 基于查询文本对关系进行评分和排序
     */
    List<ScoredRelation> scoreRelations(String query, List<Relation> relations);
    
    /**
     * 计算文本相似度
     */
    double calculateSimilarity(String text1, String text2);
    
    /**
     * 批量计算相似度
     */
    double[] calculateSimilarities(String query, List<String> texts);
    
    /**
     * 获取文本嵌入向量
     */
    float[] getEmbedding(String text);
    
    /**
     * 批量获取嵌入向量
     */
    List<float[]> getEmbeddings(List<String> texts);
}