package com.xixi.agent.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AgentWorkflowEvent {
    private String eventName;
    private Map<String, Object> payload = new HashMap<>();

    public static AgentWorkflowEvent of(String eventName, String stage, String message) {
        AgentWorkflowEvent event = new AgentWorkflowEvent();
        event.setEventName(eventName);
        event.getPayload().put("stage", stage);
        event.getPayload().put("message", message);
        return event;
    }

    public static AgentWorkflowEvent token(String text) {
        AgentWorkflowEvent event = new AgentWorkflowEvent();
        event.setEventName("token");
        event.getPayload().put("text", text);
        return event;
    }
}
