package com.tog.graph.api;

import com.tog.graph.api.dto.*;
import com.tog.graph.service.*;
import com.tog.graph.reasoning.ReasoningResult;
import com.tog.graph.agent.*;
import com.tog.graph.search.ScoredEntity;
import com.tog.graph.factory.GraphServiceFactory;
import com.tog.graph.factory.FactoryException;
import com.tog.graph.config.GraphConfig;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Graph Traveler REST API Controller
 * 提供图推理和知识发现的 REST API 接口
 */
@RestController
@RequestMapping("/api/v1/graph")
@CrossOrigin(origins = "*")
public class GraphTravelerApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphTravelerApiController.class);
    
    private final GraphReasoningService reasoningService;
    private final MultiAgentCoordinator agentCoordinator;
    
    public GraphTravelerApiController() throws FactoryException {
        GraphConfig config = new GraphConfig();
        this.reasoningService = GraphServiceFactory.createGraphReasoningService(config);
        this.agentCoordinator = new MultiAgentCoordinator();
        
        initializeServices();
    }
    
    private void initializeServices() {
        try {
            reasoningService.initialize();
            initializeAgents();
            logger.info("Graph Traveler API services initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize services", e);
            throw new RuntimeException("Service initialization failed", e);
        }
    }
    
    private void initializeAgents() {
        try {
            var database = reasoningService.getGraphDatabase();
            var searchEngine = reasoningService.getSearchEngine();

            agentCoordinator.registerAgent(new EntitySearchAgent(searchEngine));
            agentCoordinator.registerAgent(new RelationshipAnalysisAgent(database));
            agentCoordinator.initializeAll();
            
            logger.info("Multi-agent system initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize multi-agent system", e);
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        try {
            ServiceStatus status = reasoningService.getStatus();
            HealthResponse response = new HealthResponse(
                status.isFullyReady() ? "healthy" : "degraded",
                status.isDatabaseConnected(),
                status.isSearchEngineReady(),
                status.isReasonerReady(),
                status.isSchemaAwareReasonerReady()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HealthResponse("unhealthy", false, false, false, false));
        }
    }
    
    /**
     * 实体搜索接口
     */
    @PostMapping("/search/entities")
    public ResponseEntity<EntitySearchResponse> searchEntities(@RequestBody EntitySearchRequest request) {
        try {
            logger.info("Entity search request: {}", request.getQuery());
            
            EntitySearchResult result = reasoningService.searchEntities(
                request.getQuery(), 
                request.getLimit() != null ? request.getLimit() : 10
            );
            
            List<EntityDto> entities = result.getEntities().stream()
                .map(this::convertToEntityDto)
                .collect(Collectors.toList());
            
            EntitySearchResponse response = new EntitySearchResponse(
                true,
                entities,
                result.getCount(),
                0L // 暂时设为0，因为原始类没有执行时间字段
            );
            
            return ResponseEntity.ok(response);
            
        } catch (ServiceException e) {
            logger.error("Entity search failed", e);
            return ResponseEntity.badRequest()
                .body(new EntitySearchResponse(false, Collections.emptyList(), 0, 0, e.getMessage()));
        }
    }
    
    /**
     * 智能推理查询接口
     */
    @PostMapping("/reasoning/schema-aware")
    public ResponseEntity<ReasoningResponse> performSchemaAwareReasoning(@RequestBody ReasoningRequest request) {
        try {
            logger.info("Schema-aware reasoning request: {}", request.getQuestion());
            
            ReasoningResult result = reasoningService.performSchemaAwareReasoning(request.getQuestion());
            
            // 转换推理步骤为字符串列表
            List<String> reasoningSteps = result.getReasoningPath().stream()
                .map(step -> step.toString())
                .collect(Collectors.toList());
            
            ReasoningResponse response = new ReasoningResponse(
                true,
                result.getAnswer(),
                0.8, // 默认置信度，因为原始类没有置信度字段
                reasoningSteps,
                result.getEvidences(), // 使用证据作为源实体
                0L, // 暂时设为0，因为原始类没有执行时间字段
                "schema-aware"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (ServiceException e) {
            logger.error("Schema-aware reasoning failed", e);
            return ResponseEntity.badRequest()
                .body(new ReasoningResponse(false, null, 0.0, Collections.emptyList(), 
                    Collections.emptyList(), 0, "schema-aware", e.getMessage()));
        }
    }
    
    /**
     * 标准推理查询接口
     */
    @PostMapping("/reasoning/standard")
    public ResponseEntity<ReasoningResponse> performStandardReasoning(@RequestBody ReasoningRequest request) {
        try {
            logger.info("Standard reasoning request: {}", request.getQuestion());
            
            ReasoningResult result = reasoningService.performReasoning(request.getQuestion());
            
            // 转换推理步骤为字符串列表
            List<String> reasoningSteps = result.getReasoningPath().stream()
                .map(step -> step.toString())
                .collect(Collectors.toList());
            
            ReasoningResponse response = new ReasoningResponse(
                true,
                result.getAnswer(),
                0.8, // 默认置信度，因为原始类没有置信度字段
                reasoningSteps,
                result.getEvidences(), // 使用证据作为源实体
                0L, // 暂时设为0，因为原始类没有执行时间字段
                "standard"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (ServiceException e) {
            logger.error("Standard reasoning failed", e);
            return ResponseEntity.badRequest()
                .body(new ReasoningResponse(false, null, 0.0, Collections.emptyList(), 
                    Collections.emptyList(), 0, "standard", e.getMessage()));
        }
    }
    
    /**
     * 多智能体协作查询接口
     */
    @PostMapping("/agents/collaborative-query")
    public ResponseEntity<AgentResponse> performCollaborativeQuery(@RequestBody AgentRequest request) {
        try {
            logger.info("Collaborative query request: {}", request.getQuery());
            
            Map<String, Object> context = request.getContext() != null ? request.getContext() : new HashMap<>();
            
            // 根据查询类型选择不同的处理策略
            AgentResult result;
            if (request.getQuery().contains("关系") && (request.getQuery().contains("和") || request.getQuery().contains("与"))) {
                result = performRelationshipQuery(request.getQuery(), context);
            } else if (request.getQuery().contains("是什么") || request.getQuery().contains("介绍")) {
                result = performEntityQuery(request.getQuery(), context);
            } else {
                result = performGeneralQuery(request.getQuery(), context);
            }
            
            AgentResponse response = new AgentResponse(
                result.isSuccess(),
                result.getResult(),
                result.getMetadata(),
                result.getExecutionTime(),
                result.getError()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Collaborative query failed", e);
            return ResponseEntity.badRequest()
                .body(new AgentResponse(false, null, Collections.emptyMap(), 0, e.getMessage()));
        }
    }
    
    /**
     * 单个智能体任务执行接口
     */
    @PostMapping("/agents/{agentType}/execute")
    public ResponseEntity<AgentResponse> executeAgentTask(
            @PathVariable String agentType,
            @RequestBody AgentTaskRequest request) {
        try {
            logger.info("Agent task request - Type: {}, Task: {}", agentType, request.getTask());
            
            Map<String, Object> context = request.getContext() != null ? request.getContext() : new HashMap<>();
            
            AgentResult result = agentCoordinator.executeTask(agentType, request.getTask(), context);
            
            AgentResponse response = new AgentResponse(
                result.isSuccess(),
                result.getResult(),
                result.getMetadata(),
                result.getExecutionTime(),
                result.getError()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Agent task execution failed", e);
            return ResponseEntity.badRequest()
                .body(new AgentResponse(false, null, Collections.emptyMap(), 0, e.getMessage()));
        }
    }
    
    /**
     * Schema 信息接口
     */
    @GetMapping("/schema")
    public ResponseEntity<SchemaResponse> getSchemaInfo() {
        try {
            SchemaInfo schemaInfo = reasoningService.getSchemaInfo();
            
            // 转换节点类型和关系类型为字符串集合
            Set<String> nodeTypeNames = schemaInfo.getSchema() != null ? 
                schemaInfo.getSchema().getNodeTypeNames() : Collections.emptySet();
            
            Set<String> relationshipTypeNames = schemaInfo.getSchema() != null ?
                schemaInfo.getSchema().getRelationshipTypes().stream()
                    .map(rel -> rel.getType())
                    .collect(Collectors.toSet()) : Collections.emptySet();
            
            SchemaResponse response = new SchemaResponse(
                schemaInfo.isAvailable(),
                schemaInfo.getSummary(),
                nodeTypeNames,
                relationshipTypeNames,
                schemaInfo.getSchema() != null ? schemaInfo.getSchema().getTotalNodes() : 0,
                schemaInfo.getSchema() != null ? schemaInfo.getSchema().getTotalRelationships() : 0
            );
            
            return ResponseEntity.ok(response);
            
        } catch (ServiceException e) {
            logger.error("Schema info retrieval failed", e);
            return ResponseEntity.badRequest()
                .body(new SchemaResponse(false, e.getMessage(), Collections.emptySet(), 
                    Collections.emptySet(), 0, 0));
        }
    }
    
    // 私有辅助方法
    private EntityDto convertToEntityDto(ScoredEntity scoredEntity) {
        var entity = scoredEntity.getEntity();
        return new EntityDto(
            entity.getId(),
            entity.getName(),
            entity.getType(),
            entity.getProperties(),
            scoredEntity.getScore()
        );
    }
    
    private AgentResult performRelationshipQuery(String query, Map<String, Object> context) {
        // 简化的关系查询逻辑
        String[] entities = extractEntitiesFromQuestion(query);
        
        if (entities.length >= 1) {
            // 搜索第一个实体
            AgentResult searchResult = agentCoordinator.executeTask(
                "entity_search", entities[0], Map.of("limit", 3)
            );
            
            if (searchResult.isSuccess() && searchResult.getMetadata().containsKey("entities")) {
                @SuppressWarnings("unchecked")
                List<ScoredEntity> foundEntities = (List<ScoredEntity>) searchResult.getMetadata().get("entities");
                
                if (!foundEntities.isEmpty()) {
                    String entityId = foundEntities.get(0).getEntity().getId();
                    return agentCoordinator.executeTask(
                        "relationship_analysis", "分析实体关系", 
                        Map.of("entity_id", entityId)
                    );
                }
            }
        }
        
        return new AgentResult(false, null, "无法提取有效实体进行关系分析", Collections.emptyMap(), 0);
    }
    
    private AgentResult performEntityQuery(String query, Map<String, Object> context) {
        return agentCoordinator.executeTask("entity_search", query, Map.of("limit", 5));
    }
    
    private AgentResult performGeneralQuery(String query, Map<String, Object> context) {
        return agentCoordinator.executeTask("entity_search", query, Map.of("limit", 10));
    }
    
    private String[] extractEntitiesFromQuestion(String question) {
        String[] words = question.split("[\\s，。！？、的和与]+");
        List<String> entities = new ArrayList<>();
        
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 4 && 
                word.matches("[\u4e00-\u9fa5]+") && 
                !word.matches(".*[关系什么怎么样哪里为什么].*")) {
                entities.add(word);
            }
        }
        
        return entities.toArray(new String[0]);
    }
}