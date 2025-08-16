package com.tog.graph.service;

import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.embedding.EmbeddingService;
import com.tog.graph.llm.LLMService;
import com.tog.graph.reasoning.*;
import com.tog.graph.search.SearchEngine;
import com.tog.graph.search.ScoredEntity;
import com.tog.graph.schema.GraphSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 图推理业务服务层
 * 封装所有推理相关的业务逻辑，保证数据库中立性
 */
public class GraphReasoningService {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphReasoningService.class);
    
    private final GraphDatabase database;
    private final SearchEngine searchEngine;
    private final GraphReasoner reasoner;
    private final SchemaAwareGraphReasoner schemaAwareReasoner;
    private final ReasoningConfig reasoningConfig;
    
    public GraphReasoningService(GraphDatabase database, 
                                SearchEngine searchEngine,
                                EmbeddingService embeddingService,
                                LLMService llmService,
                                GraphConfig config) {
        this.database = database;
        this.searchEngine = searchEngine;
        
        // 创建推理配置
        this.reasoningConfig = createReasoningConfig(config);
        
        // 创建推理器
        this.reasoner = new GraphReasoner(database, searchEngine, llmService, reasoningConfig);
        this.schemaAwareReasoner = new SchemaAwareGraphReasoner(database, searchEngine, llmService, reasoningConfig);
    }
    
    /**
     * 初始化服务（包括Schema分析）
     */
    public void initialize() throws ServiceException {
        try {
            logger.info("Initializing graph reasoning service...");
            searchEngine.initialize();
            logger.info("Graph reasoning service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize graph reasoning service", e);
            throw new ServiceException("Service initialization failed", e);
        }
    }
    
    /**
     * 搜索实体
     */
    public EntitySearchResult searchEntities(String query, int limit) throws ServiceException {
        try {
            if (query == null || query.trim().isEmpty()) {
                throw new ServiceException("Search query cannot be empty");
            }
            
            logger.debug("Searching entities for query: {}", query);
            List<ScoredEntity> entities = searchEngine.searchEntities(query.trim(), limit);
            
            return new EntitySearchResult(query, entities, searchEngine.getClass().getSimpleName());
            
        } catch (Exception e) {
            logger.error("Entity search failed for query: {}", query, e);
            throw new ServiceException("Entity search failed", e);
        }
    }
    
    /**
     * 执行标准推理
     */
    public ReasoningResult performReasoning(String question) throws ServiceException {
        try {
            if (question == null || question.trim().isEmpty()) {
                throw new ServiceException("Reasoning question cannot be empty");
            }
            
            logger.debug("Performing standard reasoning for: {}", question);
            return reasoner.reason(question.trim());
            
        } catch (Exception e) {
            logger.error("Standard reasoning failed for question: {}", question, e);
            throw new ServiceException("Standard reasoning failed", e);
        }
    }
    
    /**
     * 执行基于Schema的智能推理
     */
    public ReasoningResult performSchemaAwareReasoning(String question) throws ServiceException {
        try {
            if (question == null || question.trim().isEmpty()) {
                throw new ServiceException("Reasoning question cannot be empty");
            }
            
            logger.debug("Performing schema-aware reasoning for: {}", question);
            return schemaAwareReasoner.reason(question.trim());
            
        } catch (Exception e) {
            logger.error("Schema-aware reasoning failed for question: {}", question, e);
            throw new ServiceException("Schema-aware reasoning failed", e);
        }
    }
    
    /**
     * 获取数据库Schema信息
     */
    public SchemaInfo getSchemaInfo() throws ServiceException {
        try {
            // 尝试从高级搜索引擎获取Schema
            if (searchEngine instanceof com.tog.graph.search.AdvancedGraphSearchEngine) {
                com.tog.graph.search.AdvancedGraphSearchEngine advancedEngine = 
                    (com.tog.graph.search.AdvancedGraphSearchEngine) searchEngine;
                GraphSchema schema = advancedEngine.getSchema();
                return new SchemaInfo(schema, true);
            } else {
                return new SchemaInfo(null, false);
            }
        } catch (Exception e) {
            logger.error("Failed to get schema info", e);
            throw new ServiceException("Failed to get schema info", e);
        }
    }
    
    /**
     * 关闭服务
     */
    public void close() {
        try {
            if (database != null) {
                database.close();
                logger.info("Graph reasoning service closed");
            }
        } catch (Exception e) {
            logger.error("Error closing graph reasoning service", e);
        }
    }
    
    /**
     * 创建推理配置
     */
    private ReasoningConfig createReasoningConfig(GraphConfig config) {
        ReasoningConfig reasoningConfig = new ReasoningConfig();
        reasoningConfig.setMaxDepth(config.getMaxReasoningDepth());
        reasoningConfig.setWidth(config.getSearchWidth());
        reasoningConfig.setEntityThreshold(config.getEntitySimilarityThreshold());
        reasoningConfig.setRelationThreshold(config.getRelationSimilarityThreshold());
        reasoningConfig.setTemperature(config.getTemperature());
        reasoningConfig.setMaxTokens(config.getMaxTokens());
        return reasoningConfig;
    }
    
    /**
     * 获取服务状态
     */
    public ServiceStatus getStatus() {
        return new ServiceStatus(
            database != null,
            searchEngine != null,
            reasoner != null,
            schemaAwareReasoner != null
        );
    }
    
    /**
     * 获取图数据库实例（用于智能体系统）
     */
    public GraphDatabase getGraphDatabase() {
        return database;
    }
    
    /**
     * 获取搜索引擎实例（用于智能体系统）
     */
    public SearchEngine getSearchEngine() {
        return searchEngine;
    }
}