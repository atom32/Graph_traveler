package com.tog.graph.demo;

import com.tog.graph.embedding.OpenAIEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * 嵌入服务测试演示
 */
public class EmbeddingTestDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingTestDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=== Embedding Service Test Demo ===");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Please set OPENAI_API_KEY environment variable");
            return;
        }
        
        OpenAIEmbeddingService embeddingService = null;
        
        try {
            // 初始化嵌入服务
            System.out.println("Initializing embedding service...");
            embeddingService = new OpenAIEmbeddingService(apiKey);
            
            // 测试服务可用性
            System.out.println("Testing service availability...");
            if (!embeddingService.isAvailable()) {
                System.out.println("Embedding service is not available!");
                return;
            }
            System.out.println("✓ Embedding service is available");
            System.out.println("✓ Embedding dimension: " + embeddingService.getDimension());
            
            // 测试单个文本嵌入
            System.out.println("\n--- Single Text Embedding Test ---");
            String testText = "Albert Einstein developed the Theory of Relativity";
            System.out.println("Text: " + testText);
            
            long startTime = System.currentTimeMillis();
            float[] embedding = embeddingService.getEmbedding(testText);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✓ Embedding generated in " + duration + "ms");
            System.out.println("✓ Embedding length: " + embedding.length);
            System.out.println("✓ First 5 values: " + Arrays.toString(Arrays.copyOf(embedding, 5)));
            
            // 测试批量嵌入
            System.out.println("\n--- Batch Embedding Test ---");
            List<String> texts = Arrays.asList(
                "Albert Einstein",
                "Marie Curie", 
                "Isaac Newton",
                "Theory of Relativity",
                "Radioactivity"
            );
            
            System.out.println("Texts: " + texts);
            
            startTime = System.currentTimeMillis();
            List<float[]> embeddings = embeddingService.getEmbeddings(texts);
            duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✓ " + embeddings.size() + " embeddings generated in " + duration + "ms");
            System.out.println("✓ Average time per embedding: " + (duration / embeddings.size()) + "ms");
            
            // 测试相似度计算
            System.out.println("\n--- Similarity Test ---");
            float[] einstein = embeddings.get(0);  // Albert Einstein
            float[] curie = embeddings.get(1);     // Marie Curie
            float[] newton = embeddings.get(2);    // Isaac Newton
            float[] relativity = embeddings.get(3); // Theory of Relativity
            
            double einsteinCurie = embeddingService.cosineSimilarity(einstein, curie);
            double einsteinNewton = embeddingService.cosineSimilarity(einstein, newton);
            double einsteinRelativity = embeddingService.cosineSimilarity(einstein, relativity);
            
            System.out.println("Einstein vs Curie similarity: " + String.format("%.4f", einsteinCurie));
            System.out.println("Einstein vs Newton similarity: " + String.format("%.4f", einsteinNewton));
            System.out.println("Einstein vs Relativity similarity: " + String.format("%.4f", einsteinRelativity));
            
            // 测试缓存
            System.out.println("\n--- Cache Test ---");
            startTime = System.currentTimeMillis();
            float[] cachedEmbedding = embeddingService.getEmbedding(testText); // 应该从缓存获取
            duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✓ Cached embedding retrieved in " + duration + "ms");
            
            // 验证缓存结果一致性
            boolean isIdentical = Arrays.equals(embedding, cachedEmbedding);
            System.out.println("✓ Cache consistency: " + (isIdentical ? "PASS" : "FAIL"));
            
            // 显示缓存统计
            if (embeddingService instanceof OpenAIEmbeddingService) {
                var stats = ((OpenAIEmbeddingService) embeddingService).getCacheStats();
                System.out.println("✓ Cache stats: " + stats);
            }
            
            System.out.println("\n=== All tests completed successfully! ===");
            
        } catch (Exception e) {
            logger.error("Test failed", e);
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (embeddingService != null) {
                embeddingService.close();
            }
        }
    }
}