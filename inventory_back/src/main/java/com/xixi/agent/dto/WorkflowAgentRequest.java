package com.xixi.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "WorkflowAgentRequest", description = "WorkflowAgentRequest")
public class WorkflowAgentRequest {
    @Schema(description = "用户消息内容")
    private String message;
    @Schema(description = "会话线程ID")
    private String threadId;
}

