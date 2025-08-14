package com.tog.graph.llm;

/**
 * LLM服务接口
 */
public interface LLMService {
    
    /**
     * 生成文本
     */
    String generate(String prompt, double temperature, int maxTokens);
    
    /**
     * 批量生成
     */
    String[] generateBatch(String[] prompts, double temperature, int maxTokens);
    
    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}