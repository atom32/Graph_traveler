package com.tog.graph.core;

import com.tog.graph.schema.PropertyInfo;
import java.util.List;
import java.util.Map;

/**
 * 图数据库抽象接口
 * 提供统一的图数据库操作接口，支持多种图数据库实现
 */
public interface GraphDatabase {
    
    // === 基础连接操作 ===
    
    /**
     * 连接到图数据库
     */
    void connect();
    
    /**
     * 关闭数据库连接
     */
    void close();
    
    // === 实体操作 ===
    
    /**
     * 根据实体ID查找实体
     */
    Entity findEntity(String entityId);
    
    /**
     * 根据实体名称搜索实体
     */
    List<Entity> searchEntities(String entityName, int limit);
    
    /**
     * 根据属性搜索实体
     */
    List<Entity> searchEntitiesByProperty(String propertyName, String value, int limit);
    
    // === 关系操作 ===
    
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
    
    // === Schema发现操作 ===
    
    /**
     * 获取所有节点类型
     */
    List<String> getAllNodeTypes();
    
    /**
     * 获取所有关系类型
     */
    List<String> getAllRelationshipTypes();
    
    /**
     * 获取节点类型的属性列表
     */
    List<String> getNodeProperties(String nodeType);
    
    /**
     * 获取关系类型的属性列表
     */
    List<String> getRelationshipProperties(String relationshipType);
    
    /**
     * 获取指定类型的节点数量
     */
    long getNodeCount(String nodeType);
    
    /**
     * 获取指定类型的关系数量
     */
    long getRelationshipCount(String relationshipType);
    
    // === 统计信息 ===
    
    /**
     * 获取总节点数
     */
    long getTotalNodeCount();
    
    /**
     * 获取总关系数
     */
    long getTotalRelationshipCount();
    
    /**
     * 获取节点类型分布
     */
    Map<String, Long> getNodeTypeDistribution();
    
    /**
     * 获取关系类型分布
     */
    Map<String, Long> getRelationshipTypeDistribution();
    
    // === 属性分析 ===
    
    /**
     * 分析节点属性
     */
    List<PropertyInfo> analyzeNodeProperties(String nodeType);
    
    /**
     * 分析关系属性
     */
    List<PropertyInfo> analyzeRelationshipProperties(String relationshipType);
    
    /**
     * 获取属性样本值
     */
    List<String> getSamplePropertyValues(String nodeType, String property, int limit);
    
    // === 图遍历 ===
    
    /**
     * 查找邻居节点
     */
    List<Entity> findNeighbors(String entityId, int maxDepth);
    
    /**
     * 查找路径
     */
    List<Path> findPaths(String sourceId, String targetId, int maxDepth);
    
    /**
     * 查找半径内的实体
     */
    List<Entity> findEntitiesInRadius(String centerId, int radius);
    
    // === 自定义查询 ===
    
    /**
     * 执行自定义查询
     */
    List<Map<String, Object>> executeQuery(String query, Map<String, Object> parameters);
    
    /**
     * 批量操作
     */
    void executeBatch(List<String> queries);
    
    // === 数据库信息 ===
    
    /**
     * 获取数据库类型
     */
    String getDatabaseType();
    
    /**
     * 获取数据库版本
     */
    String getVersion();
    
    /**
     * 获取数据库详细信息
     */
    Map<String, Object> getDatabaseInfo();
}