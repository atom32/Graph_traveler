package com.tog.graph.service;

import com.tog.graph.schema.GraphSchema;

/**
 * Schema信息封装
 */
public class SchemaInfo {
    private final GraphSchema schema;
    private final boolean available;
    
    public SchemaInfo(GraphSchema schema, boolean available) {
        this.schema = schema;
        this.available = available;
    }
    
    public GraphSchema getSchema() {
        return schema;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public String getSummary() {
        if (!available || schema == null) {
            return "Schema信息不可用 - 当前使用的是简单搜索引擎";
        }
        return schema.getSummary();
    }
}