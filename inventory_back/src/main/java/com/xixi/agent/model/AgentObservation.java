package com.xixi.agent.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AgentObservation {
    private Integer step;
    private String toolName;
    private Map<String, Object> arguments;
    private AgentToolResult result;
    private LocalDateTime createTime;
}
