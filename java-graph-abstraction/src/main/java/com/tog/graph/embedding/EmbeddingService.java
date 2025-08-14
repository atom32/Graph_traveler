package com.tog.graph.embedding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 嵌入服务接口
 * 支持文本向量化和相似度计算
 */
public interface EmbeddingService {
    
    /**
     * 获取单个文本的嵌入向量
     */
    float[] getEmbedding(String text);
    
    /**
     * 批量获取文本嵌入向量
     */
    List<float[]> getEmbeddings(List<String> texts);
    
    /**
     * 异步获取嵌入向量
     */
    CompletableFuture<float[]> getEmbeddingAsync(String text);
    
    /**
     * 异步批量获取嵌入向量
     */
    CompletableFuture<List<float[]>> getEmbeddingsAsync(List<String> texts);
    
    /**
     * 计算两个向量的余弦相似度
     */
    double cosineSimilarity(float[] vector1, float[] vector2);
    
    /**
     * 计算查询向量与候选向量列表的相似度
     */
    double[] calculateSimilarities(float[] queryVector, List<float[]> candidateVectors);
    
    /**
     * 获取嵌入向量的维度
     */
    int getDimension();
    
    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
    
    /**
     * 关闭服务
     */
    void close();
}