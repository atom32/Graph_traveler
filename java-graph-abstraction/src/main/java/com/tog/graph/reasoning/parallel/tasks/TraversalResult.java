package com.tog.graph.reasoning.parallel.tasks;

import com.tog.graph.core.Entity;
import com.tog.graph.core.Relation;
import java.util.List;
import java.util.ArrayList;

/**
 * 图遍历结果类
 */
public class TraversalResult {
    private final List<Entity> entities;
    private final List<Relation> relations;
    private final boolean success;
    private final String errorMessage;
    private final double score;
    private final int depth;
    
    public TraversalResult(List<Entity> entities, List<Relation> relations) {
        this.entities = entities != null ? entities : new ArrayList<>();
        this.relations = relations != null ? relations : new ArrayList<>();
        this.success = true;
        this.errorMessage = null;
        this.score = 1.0; // 默认分数
        this.depth = 1; // 默认深度
    }
    
    public TraversalResult(String errorMessage) {
        this.entities = new ArrayList<>();
        this.relations = new ArrayList<>();
        this.success = false;
        this.errorMessage = errorMessage;
        this.score = 0.0;
        this.depth = 0;
    }
    
    // 兼容原内部类的构造函数
    public TraversalResult(Entity startEntity, List<Relation> relations, 
                          Entity endEntity, double score, int depth) {
        this.entities = new ArrayList<>();
        if (startEntity != null) this.entities.add(startEntity);
        if (endEntity != null) this.entities.add(endEntity);
        this.relations = relations != null ? relations : new ArrayList<>();
        this.success = true;
        this.errorMessage = null;
        this.score = score;
        this.depth = depth;
    }
    
    public List<Entity> getEntities() {
        return entities;
    }
    
    public List<Relation> getRelations() {
        return relations;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    // 兼容原内部类的方法
    public double getScore() {
        return score;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public Entity getStartEntity() {
        return entities.isEmpty() ? null : entities.get(0);
    }
    
    public Entity getEndEntity() {
        return entities.size() < 2 ? null : entities.get(entities.size() - 1);
    }
    
    public String getPathDescription() {
        if (entities.size() < 2) {
            return entities.isEmpty() ? "Empty path" : entities.get(0).getName();
        }
        
        StringBuilder path = new StringBuilder();
        path.append(entities.get(0).getName());
        
        for (int i = 0; i < relations.size() && i < entities.size() - 1; i++) {
            path.append(" -[").append(relations.get(i).getType()).append("]-> ");
            if (i + 1 < entities.size()) {
                path.append(entities.get(i + 1).getName());
            }
        }
        
        return path.toString();
    }
    
    @Override
    public String toString() {
        return String.format("TraversalResult[%s, score=%.3f, depth=%d]", 
                           getPathDescription(), score, depth);
    }
}