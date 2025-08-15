package com.tog.graph.service;

import com.tog.graph.search.ScoredEntity;
import java.util.List;

/**
 * 实体搜索结果
 */
public class EntitySearchResult {
    private final String query;
    private final List<ScoredEntity> entities;
    private final String searchEngineType;
    
    public EntitySearchResult(String query, List<ScoredEntity> entities, String searchEngineType) {
        this.query = query;
        this.entities = entities;
        this.searchEngineType = searchEngineType;
    }
    
    public String getQuery() {
        return query;
    }
    
    public List<ScoredEntity> getEntities() {
        return entities;
    }
    
    public String getSearchEngineType() {
        return searchEngineType;
    }
    
    public boolean isEmpty() {
        return entities == null || entities.isEmpty();
    }
    
    public int getCount() {
        return entities != null ? entities.size() : 0;
    }
    
    public boolean isAdvancedSearch() {
        return searchEngineType.contains("Advanced");
    }
}