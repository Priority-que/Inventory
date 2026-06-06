package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "AgentSessionDetailVO", description = "AI会话详情")
public class AgentSessionDetailVO {
    @Schema(description = "会话信息")
    private AgentSessionVO session;

    @Schema(description = "会话消息")
    private List<AgentMessageVO> messages;
}
