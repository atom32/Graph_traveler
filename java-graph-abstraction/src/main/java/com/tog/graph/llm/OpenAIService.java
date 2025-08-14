package com.tog.graph.llm;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI服务实现
 */
public class OpenAIService implements LLMService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    public OpenAIService(String apiKey) {
        this(apiKey, DEFAULT_API_URL, "gpt-3.5-turbo");
    }
    
    public OpenAIService(String apiKey, String apiUrl, String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
    }
    
    @Override
    public String generate(String prompt, double temperature, int maxTokens) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            
            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            
            HttpPost request = new HttpPost(apiUrl);
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonRequest, ContentType.APPLICATION_JSON));
            
            return httpClient.execute(request, response -> {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                if (response.getCode() != 200) {
                    logger.error("OpenAI API error: {}", responseBody);
                    return "Error: Failed to generate response";
                }
                
                JsonNode messageNode = jsonResponse.path("choices").get(0).path("message");
                String content = messageNode.path("content").asText();
                
                // 如果 content 为空或 null，尝试从 reasoning_content 获取
                if (content == null || content.isEmpty() || "null".equals(content)) {
                    content = messageNode.path("reasoning_content").asText();
                }
                
                // 如果还是为空，返回默认消息
                if (content == null || content.isEmpty() || "null".equals(content)) {
                    content = "LLM response content is empty";
                }
                
                return content;
            });
            
        } catch (IOException e) {
            logger.error("Error calling OpenAI API", e);
            return "Error: " + e.getMessage();
        }
    }
    
    @Override
    public String[] generateBatch(String[] prompts, double temperature, int maxTokens) {
        String[] results = new String[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            results[i] = generate(prompts[i], temperature, maxTokens);
        }
        return results;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            String testResponse = generate("Hello", 0.0, 10);
            return !testResponse.startsWith("Error:");
        } catch (Exception e) {
            return false;
        }
    }
    
    public void close() throws IOException {
        httpClient.close();
    }
}