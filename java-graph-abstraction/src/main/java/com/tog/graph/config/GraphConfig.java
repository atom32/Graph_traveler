package com.tog.graph.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 图数据库配置
 */
public class GraphConfig {
    private String databaseType = "neo4j";
    private String uri = "bolt://localhost:7687";
    private String username = "neo4j";
    private String password = "password";
    
    // OpenAI配置
    private String openaiApiKey;
    private String openaiApiUrl = "https://api.openai.com/v1/chat/completions";
    private String openaiModel = "gpt-3.5-turbo";
    
    // 嵌入服务配置
    private String embeddingServiceType = "openai";
    private String embeddingApiKey; // 单独的 embedding API key
    private String embeddingApiUrl = "https://api.openai.com/v1/embeddings";
    private String embeddingModel = "text-embedding-ada-002";
    private int embeddingCacheSize = 1000;
    
    // 搜索配置
    private String searchEngineType = "simple";
    
    // 推理配置
    private int maxReasoningDepth = 3;
    private int searchWidth = 5;
    private double entitySimilarityThreshold = 0.3;
    private double relationSimilarityThreshold = 0.2;
    
    public GraphConfig() {
        // 优先从配置文件加载
        loadProperties();
    }
    
    /**
     * 从 application.properties 文件加载配置
     */
    private void loadProperties() {
        Properties props = new Properties();
        
        // 尝试多种方式加载配置文件
        InputStream input = null;
        
        try {
            // 优先尝试加载本地配置文件
            input = getClass().getClassLoader().getResourceAsStream("application-local.properties");
            if (input != null) {
                System.out.println("🔧 Loading local configuration: application-local.properties");
            } else {
                // 方式1: 从类路径加载默认配置
                input = getClass().getClassLoader().getResourceAsStream("application.properties");
                if (input == null) {
                    // 方式2: 从当前类的包路径加载
                    input = getClass().getResourceAsStream("/application.properties");
                }
                if (input != null) {
                    System.out.println("🔧 Loading default configuration: application.properties");
                }
            }
            
            if (input != null) {
                props.load(input);
                System.out.println("✅ Successfully loaded application.properties");
                
                // 调试：打印所有加载的属性
                System.out.println("📋 All loaded properties:");
                props.forEach((key, value) -> {
                    if (key.toString().contains("key") || key.toString().contains("password")) {
                        System.out.println("  " + key + " = ***");
                    } else {
                        System.out.println("  " + key + " = " + value);
                    }
                });
                
                // 加载数据库配置
                this.databaseType = props.getProperty("graph.database.type", this.databaseType);
                this.uri = props.getProperty("graph.database.uri", this.uri);
                this.username = props.getProperty("graph.database.username", this.username);
                this.password = props.getProperty("graph.database.password", this.password);
                
                // 加载 OpenAI 配置
                String apiKey = props.getProperty("openai.api.key");
                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    this.openaiApiKey = apiKey.trim();
                    System.out.println("✅ OpenAI API Key: ***" + this.openaiApiKey.substring(Math.max(0, this.openaiApiKey.length() - 4)));
                } else {
                    System.out.println("❌ OpenAI API Key not found in properties file");
                }
                
                String apiUrl = props.getProperty("openai.api.url");
                if (apiUrl != null && !apiUrl.trim().isEmpty()) {
                    this.openaiApiUrl = apiUrl.trim();
                    System.out.println("✅ OpenAI API URL: " + this.openaiApiUrl);
                } else {
                    System.out.println("❌ Using default OpenAI API URL: " + this.openaiApiUrl);
                }
                
                String model = props.getProperty("openai.model");
                if (model != null && !model.trim().isEmpty()) {
                    this.openaiModel = model.trim();
                    System.out.println("✅ OpenAI Model: " + this.openaiModel);
                }
                
                // 加载 embedding 配置
                String embeddingApiKey = props.getProperty("embedding.api.key");
                if (embeddingApiKey != null && !embeddingApiKey.trim().isEmpty()) {
                    this.embeddingApiKey = embeddingApiKey.trim();
                    System.out.println("✅ Embedding API Key: ***" + this.embeddingApiKey.substring(Math.max(0, this.embeddingApiKey.length() - 4)));
                } else {
                    // 如果没有单独的 embedding key，使用 OpenAI key
                    this.embeddingApiKey = this.openaiApiKey;
                    System.out.println("✅ Embedding API Key (using OpenAI key): ***" + this.embeddingApiKey.substring(Math.max(0, this.embeddingApiKey.length() - 4)));
                }
                
                String embeddingUrl = props.getProperty("embedding.api.url");
                if (embeddingUrl != null && !embeddingUrl.trim().isEmpty()) {
                    this.embeddingApiUrl = embeddingUrl.trim();
                    System.out.println("✅ Embedding API URL: " + this.embeddingApiUrl);
                } else {
                    this.embeddingApiUrl = "https://api.openai.com/v1/embeddings";
                    System.out.println("✅ Embedding API URL (default): " + this.embeddingApiUrl);
                }
                
                String embeddingModel = props.getProperty("embedding.model");
                if (embeddingModel != null && !embeddingModel.trim().isEmpty()) {
                    this.embeddingModel = embeddingModel.trim();
                    System.out.println("✅ Embedding Model: " + this.embeddingModel);
                } else {
                    this.embeddingModel = "text-embedding-ada-002";
                    System.out.println("✅ Embedding Model (default): " + this.embeddingModel);
                }
                
                System.out.println("🔧 Configuration overridden with correct values:");
                System.out.println("✅ OpenAI API Key: ***" + this.openaiApiKey.substring(Math.max(0, this.openaiApiKey.length() - 4)));
                System.out.println("✅ OpenAI API URL: " + this.openaiApiUrl);
                System.out.println("✅ OpenAI Model: " + this.openaiModel);
                System.out.println("✅ Embedding API URL: " + this.embeddingApiUrl);
                
                // 确保 getter 方法返回正确的值
                System.out.println("🔍 Verification - getOpenaiApiUrl(): " + getOpenaiApiUrl());
                System.out.println("🔍 Verification - getOpenaiModel(): " + getOpenaiModel());
                
                this.openaiModel = props.getProperty("openai.model", this.openaiModel);
                
                // 加载搜索配置
                this.searchEngineType = props.getProperty("search.engine.type", this.searchEngineType);
                
                // 加载推理配置
                String maxDepth = props.getProperty("reasoning.max.depth");
                if (maxDepth != null) {
                    this.maxReasoningDepth = Integer.parseInt(maxDepth);
                }
                
                String width = props.getProperty("reasoning.width");
                if (width != null) {
                    this.searchWidth = Integer.parseInt(width);
                }
                
                String entityThreshold = props.getProperty("reasoning.entity.threshold");
                if (entityThreshold != null) {
                    this.entitySimilarityThreshold = Double.parseDouble(entityThreshold);
                }
                
                String relationThreshold = props.getProperty("reasoning.relation.threshold");
                if (relationThreshold != null) {
                    this.relationSimilarityThreshold = Double.parseDouble(relationThreshold);
                }
                
            } else {
                System.out.println("❌ application.properties file not found in classpath");
                System.out.println("Using default configuration values");
            }
        } catch (IOException e) {
            System.err.println("❌ Error loading application.properties: " + e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        
        // 最终配置摘要
        System.out.println("Configuration loaded from application.properties");
        System.out.println("OpenAI API Key: " + (this.openaiApiKey != null ? "***" + this.openaiApiKey.substring(Math.max(0, this.openaiApiKey.length() - 4)) : "not set"));
        System.out.println("OpenAI API URL: " + this.openaiApiUrl);
    }
    
    // Getters and Setters
    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }
    
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
    
    public String getOpenaiApiUrl() { return openaiApiUrl; }
    public void setOpenaiApiUrl(String openaiApiUrl) { this.openaiApiUrl = openaiApiUrl; }
    
    public String getOpenaiModel() { return openaiModel; }
    public void setOpenaiModel(String openaiModel) { this.openaiModel = openaiModel; }
    
    public String getEmbeddingServiceType() { return embeddingServiceType; }
    public void setEmbeddingServiceType(String embeddingServiceType) { this.embeddingServiceType = embeddingServiceType; }
    
    public String getEmbeddingApiKey() { return embeddingApiKey; }
    public void setEmbeddingApiKey(String embeddingApiKey) { this.embeddingApiKey = embeddingApiKey; }
    
    public String getEmbeddingApiUrl() { return embeddingApiUrl; }
    public void setEmbeddingApiUrl(String embeddingApiUrl) { this.embeddingApiUrl = embeddingApiUrl; }
    
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    
    public int getEmbeddingCacheSize() { return embeddingCacheSize; }
    public void setEmbeddingCacheSize(int embeddingCacheSize) { this.embeddingCacheSize = embeddingCacheSize; }
    
    public String getSearchEngineType() { return searchEngineType; }
    public void setSearchEngineType(String searchEngineType) { this.searchEngineType = searchEngineType; }
    
    public int getMaxReasoningDepth() { return maxReasoningDepth; }
    public void setMaxReasoningDepth(int maxReasoningDepth) { this.maxReasoningDepth = maxReasoningDepth; }
    
    public int getSearchWidth() { return searchWidth; }
    public void setSearchWidth(int searchWidth) { this.searchWidth = searchWidth; }
    
    public double getEntitySimilarityThreshold() { return entitySimilarityThreshold; }
    public void setEntitySimilarityThreshold(double entitySimilarityThreshold) { this.entitySimilarityThreshold = entitySimilarityThreshold; }
    
    public double getRelationSimilarityThreshold() { return relationSimilarityThreshold; }
    public void setRelationSimilarityThreshold(double relationSimilarityThreshold) { this.relationSimilarityThreshold = relationSimilarityThreshold; }
    
    // 兼容性方法
    public int getMaxDepth() { return maxReasoningDepth; }
    public int getWidth() { return searchWidth; }
    public double getEntityThreshold() { return entitySimilarityThreshold; }
    public double getRelationThreshold() { return relationSimilarityThreshold; }
    public double getTemperature() { return 0.0; }
    public int getMaxTokens() { return 256; }
    public int getThreadPoolSize() { return 4; }
    public int getMaxEvidences() { return 10; }
}