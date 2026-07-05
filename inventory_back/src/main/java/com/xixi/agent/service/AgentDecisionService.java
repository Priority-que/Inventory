package com.xixi.agent.service;

import com.xixi.agent.model.AgentDecision;
import com.xixi.agent.model.AgentObservation;

import java.util.List;
import java.util.Map;

public interface AgentDecisionService {
    AgentDecision decide(String userMessage,
                         List<AgentObservation> observations,
                         List<String> roleCodes,
                         String recentConversationContext,
                         Map<String, Object> previousState);

    String buildFinalAnswer(String userMessage,
                            List<AgentObservation> observations,
                            String fallbackAnswer,
                            String recentConversationContext,
                            Map<String, Object> previousState);
}
