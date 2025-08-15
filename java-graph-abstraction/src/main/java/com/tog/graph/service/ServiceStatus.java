package com.tog.graph.service;

/**
 * 服务状态
 */
public class ServiceStatus {
    private final boolean databaseConnected;
    private final boolean searchEngineReady;
    private final boolean reasonerReady;
    private final boolean schemaAwareReasonerReady;
    
    public ServiceStatus(boolean databaseConnected, boolean searchEngineReady, 
                        boolean reasonerReady, boolean schemaAwareReasonerReady) {
        this.databaseConnected = databaseConnected;
        this.searchEngineReady = searchEngineReady;
        this.reasonerReady = reasonerReady;
        this.schemaAwareReasonerReady = schemaAwareReasonerReady;
    }
    
    public boolean isDatabaseConnected() {
        return databaseConnected;
    }
    
    public boolean isSearchEngineReady() {
        return searchEngineReady;
    }
    
    public boolean isReasonerReady() {
        return reasonerReady;
    }
    
    public boolean isSchemaAwareReasonerReady() {
        return schemaAwareReasonerReady;
    }
    
    public boolean isFullyReady() {
        return databaseConnected && searchEngineReady && reasonerReady && schemaAwareReasonerReady;
    }
}