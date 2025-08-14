package com.tog.graph.reasoning;

import java.util.List;

/**
 * 推理结果
 */
public class ReasoningResult {
    private final String question;
    private final String answer;
    private final List<ReasoningStep> reasoningPath;
    private final List<String> evidences;
    
    public ReasoningResult(String question, String answer, 
                          List<ReasoningStep> reasoningPath, List<String> evidences) {
        this.question = question;
        this.answer = answer;
        this.reasoningPath = reasoningPath;
        this.evidences = evidences;
    }
    
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public List<ReasoningStep> getReasoningPath() { return reasoningPath; }
    public List<String> getEvidences() { return evidences; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(question).append("\n");
        sb.append("Answer: ").append(answer).append("\n");
        sb.append("Reasoning Path:\n");
        for (int i = 0; i < reasoningPath.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(reasoningPath.get(i)).append("\n");
        }
        return sb.toString();
    }
}