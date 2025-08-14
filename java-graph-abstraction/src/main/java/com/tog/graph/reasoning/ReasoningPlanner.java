package com.tog.graph.reasoning;

import com.tog.graph.reasoning.ReasoningPlan.ReasoningStep;
import com.tog.graph.reasoning.ReasoningPlan.ReasoningStep.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 推理规划器 - 分析问题并生成推理计划
 */
public class ReasoningPlanner {
    
    private static final Logger logger = LoggerFactory.getLogger(ReasoningPlanner.class);
    
    private final ReasoningConfig config;
    
    public ReasoningPlanner(ReasoningConfig config) {
        this.config = config;
    }
    
    /**
     * 为给定问题生成推理计划
     */
    public ReasoningPlan createPlan(String question) {
        logger.info("Creating reasoning plan for question: {}", question);
        
        // 分析问题类型
        QuestionType questionType = analyzeQuestionType(question);
        
        // 根据问题类型生成步骤
        List<ReasoningStep> steps = generateSteps(question, questionType);
        
        // 确定执行策略
        ReasoningPlan.ReasoningStrategy strategy = determineStrategy(questionType, steps.size());
        
        ReasoningPlan plan = new ReasoningPlan(question, steps, strategy);
        
        logger.info("Generated plan with {} steps using {} strategy", 
                   steps.size(), strategy);
        
        return plan;
    }
    
    /**
     * 分析问题类型
     */
    private QuestionType analyzeQuestionType(String question) {
        String lowerQuestion = question.toLowerCase();
        
        if (lowerQuestion.startsWith("who")) {
            return QuestionType.PERSON_IDENTIFICATION;
        } else if (lowerQuestion.startsWith("where")) {
            return QuestionType.LOCATION_IDENTIFICATION;
        } else if (lowerQuestion.startsWith("what")) {
            return QuestionType.CONCEPT_IDENTIFICATION;
        } else if (lowerQuestion.startsWith("when")) {
            return QuestionType.TIME_IDENTIFICATION;
        } else if (lowerQuestion.startsWith("how")) {
            return QuestionType.PROCESS_EXPLANATION;
        } else if (lowerQuestion.startsWith("why")) {
            return QuestionType.CAUSAL_REASONING;
        } else {
            return QuestionType.GENERAL_REASONING;
        }
    }
    
    /**
     * 根据问题类型生成推理步骤
     */
    private List<ReasoningStep> generateSteps(String question, QuestionType questionType) {
        List<ReasoningStep> steps = new ArrayList<>();
        
        // 1. 实体识别步骤（总是需要）
        steps.add(new ReasoningStep(
            "entity_identification",
            StepType.ENTITY_IDENTIFICATION,
            "Identify entities from the question",
            Collections.emptyList()
        ));
        
        // 2. 根据问题类型添加特定步骤
        switch (questionType) {
            case PERSON_IDENTIFICATION:
                addPersonIdentificationSteps(steps);
                break;
            case LOCATION_IDENTIFICATION:
                addLocationIdentificationSteps(steps);
                break;
            case CONCEPT_IDENTIFICATION:
                addConceptIdentificationSteps(steps);
                break;
            case CAUSAL_REASONING:
                addCausalReasoningSteps(steps);
                break;
            default:
                addGeneralReasoningSteps(steps);
        }
        
        // 3. 添加通用的最终步骤
        addFinalSteps(steps);
        
        return steps;
    }
    
    private void addPersonIdentificationSteps(List<ReasoningStep> steps) {
        steps.add(new ReasoningStep(
            "person_relation_exploration",
            StepType.RELATION_EXPLORATION,
            "Explore person-related relations",
            List.of("entity_identification")
        ));
        
        steps.add(new ReasoningStep(
            "person_attribute_search",
            StepType.SIMILARITY_CALCULATION,
            "Search for person attributes",
            List.of("person_relation_exploration")
        ));
    }
    
    private void addLocationIdentificationSteps(List<ReasoningStep> steps) {
        steps.add(new ReasoningStep(
            "location_relation_exploration",
            StepType.RELATION_EXPLORATION,
            "Explore location-related relations",
            List.of("entity_identification")
        ));
        
        steps.add(new ReasoningStep(
            "geographic_search",
            StepType.SIMILARITY_CALCULATION,
            "Search for geographic information",
            List.of("location_relation_exploration")
        ));
    }
    
    private void addConceptIdentificationSteps(List<ReasoningStep> steps) {
        steps.add(new ReasoningStep(
            "concept_relation_exploration",
            StepType.RELATION_EXPLORATION,
            "Explore concept-related relations",
            List.of("entity_identification")
        ));
        
        steps.add(new ReasoningStep(
            "concept_definition_search",
            StepType.SIMILARITY_CALCULATION,
            "Search for concept definitions",
            List.of("concept_relation_exploration")
        ));
    }
    
    private void addCausalReasoningSteps(List<ReasoningStep> steps) {
        steps.add(new ReasoningStep(
            "causal_chain_exploration",
            StepType.RELATION_EXPLORATION,
            "Explore causal chains",
            List.of("entity_identification")
        ));
        
        steps.add(new ReasoningStep(
            "cause_effect_analysis",
            StepType.SIMILARITY_CALCULATION,
            "Analyze cause-effect relationships",
            List.of("causal_chain_exploration")
        ));
    }
    
    private void addGeneralReasoningSteps(List<ReasoningStep> steps) {
        steps.add(new ReasoningStep(
            "multi_hop_exploration",
            StepType.RELATION_EXPLORATION,
            "Multi-hop graph exploration",
            List.of("entity_identification")
        ));
        
        steps.add(new ReasoningStep(
            "relevance_scoring",
            StepType.SIMILARITY_CALCULATION,
            "Score relation relevance",
            List.of("multi_hop_exploration")
        ));
    }
    
    private void addFinalSteps(List<ReasoningStep> steps) {
        // 找到所有非最终步骤作为依赖
        List<String> allPreviousSteps = steps.stream()
                .map(ReasoningStep::getStepId)
                .toList();
        
        steps.add(new ReasoningStep(
            "evidence_collection",
            StepType.EVIDENCE_COLLECTION,
            "Collect and organize evidence",
            allPreviousSteps
        ));
        
        steps.add(new ReasoningStep(
            "answer_generation",
            StepType.ANSWER_GENERATION,
            "Generate final answer using LLM",
            List.of("evidence_collection")
        ));
        
        steps.add(new ReasoningStep(
            "result_validation",
            StepType.VALIDATION,
            "Validate reasoning result",
            List.of("answer_generation")
        ));
    }
    
    /**
     * 确定执行策略
     */
    private ReasoningPlan.ReasoningStrategy determineStrategy(QuestionType questionType, int stepCount) {
        // 简单的策略选择逻辑
        if (stepCount <= 3) {
            return ReasoningPlan.ReasoningStrategy.SEQUENTIAL;
        } else if (questionType == QuestionType.CAUSAL_REASONING) {
            return ReasoningPlan.ReasoningStrategy.ADAPTIVE;
        } else {
            return ReasoningPlan.ReasoningStrategy.PARALLEL;
        }
    }
    
    /**
     * 问题类型枚举
     */
    private enum QuestionType {
        PERSON_IDENTIFICATION,      // 人物识别
        LOCATION_IDENTIFICATION,    // 地点识别
        CONCEPT_IDENTIFICATION,     // 概念识别
        TIME_IDENTIFICATION,        // 时间识别
        PROCESS_EXPLANATION,        // 过程解释
        CAUSAL_REASONING,          // 因果推理
        GENERAL_REASONING          // 通用推理
    }
}