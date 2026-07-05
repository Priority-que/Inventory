package com.xixi.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentDecision {
    private String action;
    private String toolName;
    private Map<String, Object> arguments = new HashMap<>();
    private String userVisibleStatus;
    private String finalAnswer;

    public static AgentDecision finalAnswer(String answer) {
        AgentDecision decision = new AgentDecision();
        decision.setAction(AgentAction.FINAL_ANSWER);
        decision.setFinalAnswer(answer);
        decision.setUserVisibleStatus("正在整理回答");
        return decision;
    }

    public static AgentDecision askClarify(String question) {
        AgentDecision decision = new AgentDecision();
        decision.setAction(AgentAction.ASK_CLARIFY);
        decision.setFinalAnswer(question);
        decision.setUserVisibleStatus("需要补充信息");
        return decision;
    }
}
