package com.tog.graph.core;

import java.util.List;
import java.util.ArrayList;

/**
 * 路径表示类（数据库中立）
 * 表示图中两个节点之间的路径
 */
public class Path {
    private List<Entity> nodes;
    private List<Relation> relationships;
    private double score;
    private int length;
    
    public Path() {
        this.nodes = new ArrayList<>();
        this.relationships = new ArrayList<>();
        this.score = 0.0;
        this.length = 0;
    }
    
    public Path(List<Entity> nodes, List<Relation> relationships) {
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
        this.relationships = relationships != null ? new ArrayList<>(relationships) : new ArrayList<>();
        this.length = this.relationships.size();
        this.score = 0.0;
    }
    
    public List<Entity> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<Entity> nodes) {
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
    }
    
    public List<Relation> getRelationships() {
        return relationships;
    }
    
    public void setRelationships(List<Relation> relationships) {
        this.relationships = relationships != null ? new ArrayList<>(relationships) : new ArrayList<>();
        this.length = this.relationships.size();
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public int getLength() {
        return length;
    }
    
    public void addNode(Entity node) {
        if (node != null) {
            this.nodes.add(node);
        }
    }
    
    public void addRelationship(Relation relationship) {
        if (relationship != null) {
            this.relationships.add(relationship);
            this.length = this.relationships.size();
        }
    }
    
    public Entity getStartNode() {
        return nodes.isEmpty() ? null : nodes.get(0);
    }
    
    public Entity getEndNode() {
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
    
    public boolean isEmpty() {
        return nodes.isEmpty() && relationships.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Path{length=").append(length)
          .append(", score=").append(score)
          .append(", nodes=").append(nodes.size())
          .append(", relationships=").append(relationships.size())
          .append("}");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Path path = (Path) obj;
        return Double.compare(path.score, score) == 0 &&
               length == path.length &&
               nodes.equals(path.nodes) &&
               relationships.equals(path.relationships);
    }
    
    @Override
    public int hashCode() {
        int result = nodes.hashCode();
        result = 31 * result + relationships.hashCode();
        result = 31 * result + Double.hashCode(score);
        result = 31 * result + length;
        return result;
    }
}