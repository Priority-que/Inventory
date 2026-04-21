package com.xixi.agent.dto;

import lombok.Data;

@Data
public class WarningScanRequest {
    private Integer days = 7;
    private String threadId;
}
