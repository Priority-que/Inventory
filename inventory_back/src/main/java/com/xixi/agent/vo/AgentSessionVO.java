package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AgentSessionVO", description = "AgentSessionVO")
public class AgentSessionVO {
    @Schema(description = "AI会话主键ID")
    private Long id;

    @Schema(description = "会话编号")
    private String sessionNo;

    @Schema(description = "会话线程ID")
    private String threadId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "AI助手类型")
    private String agentType;

    @Schema(description = "当前业务意图")
    private String currentIntent;

    @Schema(description = "业务状态")
    private String status;

    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

