package com.tog.graph.factory;

import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.embedding.EmbeddingService;
import com.tog.graph.embedding.OpenAIEmbeddingService;
import com.tog.graph.llm.LLMService;
import com.tog.graph.llm.OpenAIService;
import com.tog.graph.search.AdvancedGraphSearchEngine;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.SimpleSearchEngine;
import com.tog.graph.service.GraphReasoningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图服务工厂 - 数据库中立的服务创建
 */
public class GraphServiceFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphServiceFactory.class);
    
    /**
     * 创建图推理服务
     */
    public static GraphReasoningService createGraphReasoningService(GraphConfig config) throws FactoryException {
        try {
            // 1. 创建数据库实例
            GraphDatabase database = createGraphDatabase(config);
            
            // 2. 创建嵌入服务
            EmbeddingService embeddingService = createEmbeddingService(config);
            
            // 3. 创建LLM服务
            LLMService llmService = createLLMService(config);
            
            // 4. 创建搜索引擎
            SearchEngine searchEngine = createSearchEngine(config, database, embeddingService);
            
            // 5. 创建推理服务
            return new GraphReasoningService(database, searchEngine, embeddingService, llmService, config);
            
        } catch (Exception e) {
            logger.error("Failed to create graph reasoning service", e);
            throw new FactoryException("Failed to create graph reasoning service", e);
        }
    }
    
    /**
     * 创建图数据库实例 - 数据库中立
     */
    private static GraphDatabase createGraphDatabase(GraphConfig config) throws FactoryException {
        String databaseType = config.getDatabaseType();
        
        try {
            switch (databaseType.toLowerCase()) {
                case "neo4j":
                    return createNeo4jDatabase(config);
                case "rdf":
                    return createRdfDatabase(config);
                default:
                    throw new FactoryException("Unsupported database type: " + databaseType);
            }
        } catch (Exception e) {
            logger.error("Failed to create database of type: {}", databaseType, e);
            throw new FactoryException("Failed to create database", e);
        }
    }
    
    /**
     * 创建Neo4j数据库实例
     */
    private static GraphDatabase createNeo4jDatabase(GraphConfig config) throws Exception {
        Class<?> neo4jClass = Class.forName("com.tog.graph.neo4j.Neo4jGraphDatabase");
        Object instance = neo4jClass.getDeclaredConstructor().newInstance();
        GraphDatabase database = (GraphDatabase) instance;
        
        // 连接数据库
        try {
            java.lang.reflect.Method connectMethod = neo4jClass.getMethod("connect", String.class, String.class, String.class);
            connectMethod.invoke(instance, config.getUri(), config.getUsername(), config.getPassword());
            logger.info("Connected to Neo4j database");
        } catch (Exception connectError) {
            logger.warn("Failed to connect using Neo4j-specific method, trying generic connect", connectError);
            database.connect();
        }
        
        return database;
    }
    
    /**
     * 创建RDF数据库实例
     */
    private static GraphDatabase createRdfDatabase(GraphConfig config) throws Exception {
        Class<?> rdfClass = Class.forName("com.tog.graph.rdf.RdfGraphDatabase");
        java.lang.reflect.Constructor<?> constructor = rdfClass.getConstructor(String.class);
        Object instance = constructor.newInstance(config.getUri());
        GraphDatabase database = (GraphDatabase) instance;
        database.connect();
        logger.info("Connected to RDF database");
        return database;
    }
    
    /**
     * 创建嵌入服务
     */
    private static EmbeddingService createEmbeddingService(GraphConfig config) {
        return new OpenAIEmbeddingService(
            config.getEmbeddingApiKey(),
            config.getEmbeddingApiUrl(),
            config.getEmbeddingModel(),
            config.getEmbeddingCacheSize()
        );
    }
    
    /**
     * 创建LLM服务
     */
    private static LLMService createLLMService(GraphConfig config) {
        return new OpenAIService(
            config.getOpenaiApiKey(),
            config.getOpenaiApiUrl(),
            config.getOpenaiModel()
        );
    }
    
    /**
     * 创建搜索引擎
     */
    private static SearchEngine createSearchEngine(GraphConfig config, GraphDatabase database, EmbeddingService embeddingService) {
        String searchEngineType = config.getSearchEngineType();
        
        switch (searchEngineType.toLowerCase()) {
            case "advanced":
                return new AdvancedGraphSearchEngine(database, embeddingService);
            case "simple":
            default:
                return new SimpleSearchEngine(database, embeddingService);
        }
    }
}