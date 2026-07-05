package com.xixi.agent.service;

import com.xixi.agent.model.AgentToolResult;

import java.util.List;
import java.util.Map;

public interface AgentToolExecutor {
    List<String> getToolNames();

    boolean supports(String toolName);

    AgentToolResult execute(String toolName, Map<String, Object> arguments);
}
