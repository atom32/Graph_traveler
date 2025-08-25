package com.tog.graph.api.dto;

/**
 * 健康检查响应
 */
public class HealthResponse {
    private String status;
    private boolean databaseConnected;
    private boolean searchEngineReady;
    private boolean reasonerReady;
    private boolean schemaAwareReasonerReady;
    
    public HealthResponse() {}
    
    public HealthResponse(String status, boolean databaseConnected, boolean searchEngineReady, 
                         boolean reasonerReady, boolean schemaAwareReasonerReady) {
        this.status = status;
        this.databaseConnected = databaseConnected;
        this.searchEngineReady = searchEngineReady;
        this.reasonerReady = reasonerReady;
        this.schemaAwareReasonerReady = schemaAwareReasonerReady;
    }
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public boolean isDatabaseConnected() { return databaseConnected; }
    public void setDatabaseConnected(boolean databaseConnected) { this.databaseConnected = databaseConnected; }
    
    public boolean isSearchEngineReady() { return searchEngineReady; }
    public void setSearchEngineReady(boolean searchEngineReady) { this.searchEngineReady = searchEngineReady; }
    
    public boolean isReasonerReady() { return reasonerReady; }
    public void setReasonerReady(boolean reasonerReady) { this.reasonerReady = reasonerReady; }
    
    public boolean isSchemaAwareReasonerReady() { return schemaAwareReasonerReady; }
    public void setSchemaAwareReasonerReady(boolean schemaAwareReasonerReady) { this.schemaAwareReasonerReady = schemaAwareReasonerReady; }
}