package com.tog.graph.core;

import java.util.List;
import java.util.Map;

/**
 * 图数据库抽象接口
 * 提供统一的图数据库操作接口，支持多种图数据库实现
 */
public interface GraphDatabase {
    
    /**
     * 连接到图数据库
     */
    void connect();
    
    /**
     * 关闭数据库连接
     */
    void close();
    
    /**
     * 根据实体ID查找实体
     */
    Entity findEntity(String entityId);
    
    /**
     * 根据实体名称搜索实体
     */
    List<Entity> searchEntities(String entityName, int limit);
    
    /**
     * 获取实体的所有关系
     */
    List<Relation> getEntityRelations(String entityId);
    
    /**
     * 获取实体的出边关系
     */
    List<Relation> getOutgoingRelations(String entityId);
    
    /**
     * 获取实体的入边关系
     */
    List<Relation> getIncomingRelations(String entityId);
    
    /**
     * 根据关系类型查找相关实体
     */
    List<Entity> findRelatedEntities(String entityId, String relationType);
    
    /**
     * 执行自定义查询
     */
    List<Map<String, Object>> executeQuery(String query, Map<String, Object> parameters);
    
    /**
     * 批量操作
     */
    void executeBatch(List<String> queries);
}