package com.tog.graph.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 欢迎页面控制器
 */
@RestController
public class WelcomeController {
    
    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Graph Traveler API");
        response.put("version", "1.0.0");
        response.put("description", "智能图推理与知识发现系统");
        response.put("health_check", "/api/v1/graph/health");
        response.put("documentation", "查看 API_DOCUMENTATION.md");
        response.put("test_client", "打开 test-api.html");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("健康检查", "GET /api/v1/graph/health");
        endpoints.put("实体搜索", "POST /api/v1/graph/search/entities");
        endpoints.put("智能推理", "POST /api/v1/graph/reasoning/schema-aware");
        endpoints.put("多智能体协作", "POST /api/v1/graph/agents/collaborative-query");
        endpoints.put("Schema信息", "GET /api/v1/graph/schema");
        
        response.put("endpoints", endpoints);
        
        return response;
    }
}