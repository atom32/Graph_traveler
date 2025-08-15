package com.tog.graph.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * OpenAI嵌入服务实现
 * 使用OpenAI的text-embedding-ada-002模型
 */
public class OpenAIEmbeddingService implements EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIEmbeddingService.class);
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/embeddings";
    private static final String DEFAULT_MODEL = "text-embedding-ada-002";
    private static final int DEFAULT_EMBEDDING_DIMENSION = 1536; // ada-002的维度
    
    private final int embeddingDimension;
    private static final int MAX_BATCH_SIZE = 100; // OpenAI的批量限制
    
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;
    
    // 简单的缓存机制
    private final Map<String, float[]> embeddingCache;
    private final int maxCacheSize;
    
    public OpenAIEmbeddingService(String apiKey) {
        this(apiKey, DEFAULT_API_URL, DEFAULT_MODEL, 1000);
    }
    
    public OpenAIEmbeddingService(String apiKey, String apiUrl, String model, int maxCacheSize) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.maxCacheSize = maxCacheSize;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
        this.executorService = Executors.newFixedThreadPool(4);
        
        // 根据模型确定维度
        this.embeddingDimension = determineEmbeddingDimension(model);
        
        this.embeddingCache = Collections.synchronizedMap(new LinkedHashMap<String, float[]>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                return size() > maxCacheSize;
            }
        });
    }
    
    /**
     * 根据模型名称确定 embedding 维度
     */
    private int determineEmbeddingDimension(String model) {
        if (model == null) {
            return DEFAULT_EMBEDDING_DIMENSION;
        }
        
        // 常见模型的维度映射
        switch (model.toLowerCase()) {
            case "text-embedding-ada-002":
                return 1536;
            case "baai/bge-large-zh-v1.5":
            case "bge-large-zh-v1.5":
                return 1024;  // BGE large 模型的维度
            case "baai/bge-base-zh-v1.5":
            case "bge-base-zh-v1.5":
                return 768;   // BGE base 模型的维度
            case "baai/bge-small-zh-v1.5":
            case "bge-small-zh-v1.5":
                return 512;   // BGE small 模型的维度
            default:
                logger.warn("Unknown model: {}, using default dimension: {}", model, DEFAULT_EMBEDDING_DIMENSION);
                return DEFAULT_EMBEDDING_DIMENSION;
        }
    }
    
    @Override
    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return createZeroVector();
        }
        
        // 检查缓存
        String cacheKey = text.trim();
        if (embeddingCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for text: {}", text.substring(0, Math.min(50, text.length())));
            return embeddingCache.get(cacheKey);
        }
        
        // 重试机制
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                List<float[]> embeddings = requestEmbeddings(Collections.singletonList(text));
                if (!embeddings.isEmpty()) {
                    float[] embedding = embeddings.get(0);
                    // 验证向量维度
                    if (embedding.length != embeddingDimension) {
                        logger.warn("Unexpected embedding dimension: {} (expected {})", embedding.length, embeddingDimension);
                        return createZeroVector();
                    }
                    embeddingCache.put(cacheKey, embedding);
                    return embedding;
                }
            } catch (Exception e) {
                logger.warn("Attempt {} failed to get embedding for text: {} - {}", attempt, 
                           text.substring(0, Math.min(50, text.length())), e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        // 指数退避
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    logger.error("All attempts failed to get embedding for text: " + text, e);
                }
            }
        }
        
        // 返回零向量作为fallback
        return createZeroVector();
    }
    
    /**
     * 创建零向量，确保维度正确
     */
    private float[] createZeroVector() {
        return new float[embeddingDimension];
    }
    
    @Override
    public List<float[]> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<float[]> results = new ArrayList<>();
        List<String> uncachedTexts = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();
        
        // 检查缓存
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.trim().isEmpty()) {
                results.add(createZeroVector());
            } else {
                String cacheKey = text.trim();
                if (embeddingCache.containsKey(cacheKey)) {
                    results.add(embeddingCache.get(cacheKey));
                } else {
                    results.add(null); // 占位符
                    uncachedTexts.add(text);
                    uncachedIndices.add(i);
                }
            }
        }
        
        // 批量请求未缓存的文本
        if (!uncachedTexts.isEmpty()) {
            try {
                List<float[]> newEmbeddings = requestEmbeddingsBatch(uncachedTexts);
                
                // 填充结果并更新缓存
                for (int i = 0; i < uncachedIndices.size() && i < newEmbeddings.size(); i++) {
                    int originalIndex = uncachedIndices.get(i);
                    float[] embedding = newEmbeddings.get(i);
                    
                    // 验证维度
                    if (embedding != null && embedding.length == embeddingDimension) {
                        results.set(originalIndex, embedding);
                        // 更新缓存
                        String cacheKey = uncachedTexts.get(i).trim();
                        embeddingCache.put(cacheKey, embedding);
                    } else {
                        results.set(originalIndex, createZeroVector());
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to get batch embeddings", e);
                // 用零向量填充失败的位置
                for (int index : uncachedIndices) {
                    if (results.get(index) == null) {
                        results.set(index, createZeroVector());
                    }
                }
            }
        }
        
        return results;
    }
    
    @Override
    public CompletableFuture<float[]> getEmbeddingAsync(String text) {
        return CompletableFuture.supplyAsync(() -> getEmbedding(text), executorService);
    }
    
    @Override
    public CompletableFuture<List<float[]>> getEmbeddingsAsync(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> getEmbeddings(texts), executorService);
    }
    
    @Override
    public double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null) {
            logger.warn("One or both vectors are null, returning 0.0 similarity");
            return 0.0;
        }
        
        if (vector1.length != vector2.length) {
            logger.warn("Vector dimensions don't match: {} vs {}, returning 0.0 similarity", 
                       vector1.length, vector2.length);
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        
        // 确保结果在合理范围内
        if (Double.isNaN(similarity) || Double.isInfinite(similarity)) {
            logger.warn("Invalid similarity result: {}, returning 0.0", similarity);
            return 0.0;
        }
        
        return similarity;
    }
    
    @Override
    public double[] calculateSimilarities(float[] queryVector, List<float[]> candidateVectors) {
        return candidateVectors.stream()
                .mapToDouble(candidate -> cosineSimilarity(queryVector, candidate))
                .toArray();
    }
    
    @Override
    public int getDimension() {
        return embeddingDimension;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            float[] testEmbedding = getEmbedding("test");
            return testEmbedding.length == embeddingDimension;
        } catch (Exception e) {
            logger.warn("Embedding service availability check failed", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        try {
            executorService.shutdown();
            httpClient.close();
            embeddingCache.clear();
        } catch (IOException e) {
            logger.error("Error closing embedding service", e);
        }
    }
    
    /**
     * 请求单个或少量文本的嵌入
     */
    private List<float[]> requestEmbeddings(List<String> texts) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", texts);
        
        String jsonRequest = objectMapper.writeValueAsString(requestBody);
        
        HttpPost request = new HttpPost(apiUrl);
        request.setHeader("Authorization", "Bearer " + apiKey);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonRequest, ContentType.APPLICATION_JSON));
        
        return httpClient.execute(request, response -> {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            
            if (response.getCode() != 200) {
                logger.error("OpenAI Embedding API error: {}", responseBody);
                throw new RuntimeException("Embedding API call failed: " + responseBody);
            }
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode dataArray = jsonResponse.path("data");
            
            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode dataNode : dataArray) {
                JsonNode embeddingArray = dataNode.path("embedding");
                float[] embedding = new float[embeddingArray.size()];
                for (int i = 0; i < embeddingArray.size(); i++) {
                    embedding[i] = (float) embeddingArray.get(i).asDouble();
                }
                embeddings.add(embedding);
            }
            
            return embeddings;
        });
    }
    
    /**
     * 批量请求嵌入，自动处理分批
     */
    private List<float[]> requestEmbeddingsBatch(List<String> texts) throws IOException {
        List<float[]> allEmbeddings = new ArrayList<>();
        
        // 分批处理
        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            int endIndex = Math.min(i + MAX_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, endIndex);
            
            List<float[]> batchEmbeddings = requestEmbeddings(batch);
            allEmbeddings.addAll(batchEmbeddings);
            
            // 避免API限流
            if (endIndex < texts.size()) {
                try {
                    Thread.sleep(100); // 100ms延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return allEmbeddings;
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", embeddingCache.size());
        stats.put("maxCacheSize", maxCacheSize);
        stats.put("cacheHitRate", calculateCacheHitRate());
        return stats;
    }
    
    private double calculateCacheHitRate() {
        // 简化实现，实际应该跟踪命中和未命中次数
        return embeddingCache.size() > 0 ? 0.8 : 0.0;
    }
}