package com.xixi.agent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AgentToolResult {
    private String toolName;
    private boolean success;
    private String summary;
    private Map<String, Object> facts = new HashMap<>();
    private List<Object> items = new ArrayList<>();
    private List<String> evidence = new ArrayList<>();
    private String error;

    public static AgentToolResult success(String toolName, String summary) {
        AgentToolResult result = new AgentToolResult();
        result.setToolName(toolName);
        result.setSuccess(true);
        result.setSummary(summary);
        return result;
    }

    public static AgentToolResult failure(String toolName, String error) {
        AgentToolResult result = new AgentToolResult();
        result.setToolName(toolName);
        result.setSuccess(false);
        result.setSummary(error);
        result.setError(error);
        return result;
    }
}
