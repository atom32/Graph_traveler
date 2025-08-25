package com.tog.graph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph Traveler API 应用程序主类
 * 基于 Spring Boot 的 REST API 服务
 */
@SpringBootApplication
public class GraphTravelerApiApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphTravelerApiApplication.class);
    
    public static void main(String[] args) {
        logger.info("Starting Graph Traveler API Application...");
        
        try {
            SpringApplication.run(GraphTravelerApiApplication.class, args);
            logger.info("Graph Traveler API Application started successfully!");
        } catch (Exception e) {
            logger.error("Failed to start Graph Traveler API Application", e);
            System.exit(1);
        }
    }
}