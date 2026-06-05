package com.xixi.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "WarningScanRequest", description = "WarningScanRequest")
public class WarningScanRequest {
    @Schema(description = "统计天数")
    private Integer days = 7;
    @Schema(description = "会话线程ID")
    private String threadId;
}

