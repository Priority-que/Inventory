package com.xixi.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentSessionVO {
    private Long id;

    private String sessionNo;

    private String threadId;

    private Long userId;

    private String title;

    private String agentType;

    private String currentIntent;

    private String status;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;
}
