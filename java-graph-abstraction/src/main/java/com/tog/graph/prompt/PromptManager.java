package com.tog.graph.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt模板管理器
 * 负责加载和管理各种prompt模板
 */
public class PromptManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptManager.class);
    
    private static final String PROMPT_BASE_PATH = "/prompts/";
    
    // 缓存已加载的prompt模板
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();
    
    private static PromptManager instance;
    
    private PromptManager() {
        // 私有构造函数，实现单例模式
    }
    
    public static PromptManager getInstance() {
        if (instance == null) {
            synchronized (PromptManager.class) {
                if (instance == null) {
                    instance = new PromptManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取prompt模板
     */
    public String getPrompt(String promptName) {
        return promptCache.computeIfAbsent(promptName, this::loadPrompt);
    }
    
    /**
     * 获取prompt模板并填充参数
     */
    public String getPrompt(String promptName, Map<String, String> parameters) {
        String template = getPrompt(promptName);
        return fillTemplate(template, parameters);
    }
    
    /**
     * 从资源文件加载prompt模板
     */
    private String loadPrompt(String promptName) {
        String fileName = promptName.endsWith(".txt") ? promptName : promptName + ".txt";
        String resourcePath = PROMPT_BASE_PATH + fileName;
        
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.error("Prompt file not found: {}", resourcePath);
                return "Prompt template not found: " + promptName;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            String prompt = content.toString().trim();
            logger.debug("Loaded prompt template: {} ({} characters)", promptName, prompt.length());
            return prompt;
            
        } catch (Exception e) {
            logger.error("Failed to load prompt template: {}", promptName, e);
            return "Error loading prompt template: " + promptName;
        }
    }
    
    /**
     * 填充模板参数
     */
    private String fillTemplate(String template, Map<String, String> parameters) {
        String result = template;
        
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        promptCache.clear();
        logger.info("Prompt cache cleared");
    }
    
    /**
     * 重新加载指定的prompt
     */
    public void reloadPrompt(String promptName) {
        promptCache.remove(promptName);
        logger.info("Prompt reloaded: {}", promptName);
    }
    
    /**
     * 获取所有已缓存的prompt名称
     */
    public java.util.Set<String> getCachedPromptNames() {
        return promptCache.keySet();
    }
    
    /**
     * 便捷方法：创建参数Map
     */
    public static Map<String, String> params() {
        return new HashMap<>();
    }
    
    /**
     * 便捷方法：添加参数
     */
    public static Map<String, String> params(String key, String value) {
        Map<String, String> params = new HashMap<>();
        params.put(key, value);
        return params;
    }
    
    /**
     * 便捷方法：添加多个参数
     */
    public static Map<String, String> params(String k1, String v1, String k2, String v2) {
        Map<String, String> params = new HashMap<>();
        params.put(k1, v1);
        params.put(k2, v2);
        return params;
    }
    
    /**
     * 便捷方法：添加多个参数
     */
    public static Map<String, String> params(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> params = new HashMap<>();
        params.put(k1, v1);
        params.put(k2, v2);
        params.put(k3, v3);
        return params;
    }
}