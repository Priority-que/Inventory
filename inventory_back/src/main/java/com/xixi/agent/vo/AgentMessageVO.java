package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AgentMessageVO", description = "AgentMessageVO")
public class AgentMessageVO {
    @Schema(description = "AI消息主键ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "会话线程ID")
    private String threadId;

    @Schema(description = "消息角色")
    private String messageRole;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

