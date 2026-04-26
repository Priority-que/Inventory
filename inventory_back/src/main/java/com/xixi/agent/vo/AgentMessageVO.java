package com.xixi.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentMessageVO {
    private Long id;

    private Long sessionId;

    private String threadId;

    private String messageRole;

    private String messageType;

    private String content;

    private LocalDateTime createTime;
}
