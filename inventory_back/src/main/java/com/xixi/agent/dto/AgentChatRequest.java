package com.xixi.agent.dto;

import lombok.Data;

@Data
public class AgentChatRequest {
    private String message;
    private String threadId;
}
