package com.tog.graph;

import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.embedding.EmbeddingService;
import com.tog.graph.embedding.OpenAIEmbeddingService;
import com.tog.graph.llm.LLMService;
import com.tog.graph.llm.OpenAIService;
import com.tog.graph.neo4j.Neo4jGraphDatabase;
import com.tog.graph.reasoning.GraphReasoner;
import com.tog.graph.reasoning.ReasoningConfig;
import com.tog.graph.reasoning.ReasoningResult;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.SimpleSearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 图推理系统主类
 * 整合所有组件，提供统一的推理接口
 */
public class GraphReasoningSystem {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphReasoningSystem.class);
    
    private final GraphConfig config;
    private GraphDatabase graphDatabase;
    private EmbeddingService embeddingService;
    private SearchEngine searchEngine;
    private LLMService llmService;
    private GraphReasoner reasoner;
    
    public GraphReasoningSystem(GraphConfig config) {
        this.config = config;
        initialize();
    }
    
    /**
     * 初始化系统
     */
    private void initialize() {
        try {
            // 初始化图数据库
            initializeGraphDatabase();
            
            // 初始化嵌入服务
            initializeEmbeddingService();
            
            // 初始化搜索引擎
            initializeSearchEngine();
            
            // 初始化LLM服务
            initializeLLMService();
            
            // 初始化推理器
            initializeReasoner();
            
            logger.info("Graph reasoning system initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize graph reasoning system", e);
            throw new RuntimeException("System initialization failed", e);
        }
    }
    
    private void initializeGraphDatabase() {
        switch (config.getDatabaseType().toLowerCase()) {
            case "neo4j":
                Neo4jGraphDatabase neo4jDb = new Neo4jGraphDatabase();
                neo4jDb.connect(config.getUri(), config.getUsername(), config.getPassword());
                graphDatabase = neo4jDb;
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + config.getDatabaseType());
        }
        
        graphDatabase.connect();
        logger.info("Graph database connected: {}", config.getDatabaseType());
    }
    
    private void initializeEmbeddingService() {
        if (config.getOpenaiApiKey() == null || config.getOpenaiApiKey().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is required for embedding service");
        }
        
        switch (config.getEmbeddingServiceType().toLowerCase()) {
            case "openai":
                embeddingService = new OpenAIEmbeddingService(
                    config.getEmbeddingApiKey(), // 使用单独的 embedding API key
                    config.getEmbeddingApiUrl(),
                    config.getEmbeddingModel(),
                    config.getEmbeddingCacheSize()
                );
                break;
            default:
                throw new IllegalArgumentException("Unsupported embedding service type: " + config.getEmbeddingServiceType());
        }
        
        if (!embeddingService.isAvailable()) {
            logger.warn("Embedding service is not available, some features may not work properly");
        } else {
            logger.info("Embedding service initialized: {} (dimension: {})", 
                       config.getEmbeddingServiceType(), embeddingService.getDimension());
        }
    }
    
    private void initializeSearchEngine() {
        switch (config.getSearchEngineType().toLowerCase()) {
            case "simple":
                searchEngine = new SimpleSearchEngine(graphDatabase, embeddingService);
                break;
            default:
                throw new IllegalArgumentException("Unsupported search engine type: " + config.getSearchEngineType());
        }
        
        searchEngine.initialize();
        logger.info("Search engine initialized: {}", config.getSearchEngineType());
    }
    
    private void initializeLLMService() {
        if (config.getOpenaiApiKey() == null || config.getOpenaiApiKey().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is required");
        }
        
        // 调试：显示传递给 LLM 服务的参数
        System.out.println("🔍 LLM Service Parameters:");
        System.out.println("  API Key: ***" + config.getOpenaiApiKey().substring(Math.max(0, config.getOpenaiApiKey().length() - 4)));
        System.out.println("  API URL: " + config.getOpenaiApiUrl());
        System.out.println("  Model: " + config.getOpenaiModel());
        
        llmService = new OpenAIService(
            config.getOpenaiApiKey(),
            config.getOpenaiApiUrl(),
            config.getOpenaiModel()
        );
        
        if (!llmService.isAvailable()) {
            logger.warn("LLM service is not available, some features may not work");
        } else {
            logger.info("LLM service initialized: {}", config.getOpenaiModel());
        }
    }
    
    private void initializeReasoner() {
        ReasoningConfig reasoningConfig = new ReasoningConfig(
            config.getMaxReasoningDepth(),
            config.getSearchWidth(),
            config.getEntitySimilarityThreshold(),
            config.getRelationSimilarityThreshold()
        );
        reasoner = new GraphReasoner(graphDatabase, searchEngine, llmService, reasoningConfig);
        logger.info("Graph reasoner initialized with depth={}, width={}", 
                   config.getMaxReasoningDepth(), config.getSearchWidth());
    }
    
    /**
     * 执行推理
     */
    public ReasoningResult reason(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be null or empty");
        }
        
        logger.info("Starting reasoning for question: {}", question);
        
        try {
            ReasoningResult result = reasoner.reason(question);
            logger.info("Reasoning completed successfully");
            return result;
        } catch (Exception e) {
            logger.error("Reasoning failed for question: " + question, e);
            throw new RuntimeException("Reasoning failed", e);
        }
    }
    
    /**
     * 关闭系统
     */
    public void close() {
        try {
            if (reasoner != null) {
                reasoner.shutdown();
            }
            
            if (embeddingService != null) {
                embeddingService.close();
            }
            
            if (graphDatabase != null) {
                graphDatabase.close();
            }
            
            if (llmService instanceof OpenAIService) {
                ((OpenAIService) llmService).close();
            }
            
            logger.info("Graph reasoning system closed");
            
        } catch (Exception e) {
            logger.error("Error closing graph reasoning system", e);
        }
    }
    
    // Getters for testing and advanced usage
    public GraphDatabase getGraphDatabase() { return graphDatabase; }
    public EmbeddingService getEmbeddingService() { return embeddingService; }
    public SearchEngine getSearchEngine() { return searchEngine; }
    public LLMService getLlmService() { return llmService; }
    public GraphReasoner getReasoner() { return reasoner; }
    
    /**
     * 异步推理接口
     */
    public CompletableFuture<ReasoningResult> reasonAsync(String question) {
        return reasoner.reasonAsync(question);
    }
}