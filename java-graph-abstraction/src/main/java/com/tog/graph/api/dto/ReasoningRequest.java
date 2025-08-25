package com.tog.graph.api.dto;

/**
 * 推理查询请求
 */
public class ReasoningRequest {
    private String question;
    private String context;
    
    public ReasoningRequest() {}
    
    public ReasoningRequest(String question) {
        this.question = question;
    }
    
    public ReasoningRequest(String question, String context) {
        this.question = question;
        this.context = context;
    }
    
    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}