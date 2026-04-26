package com.xixi.agent.vo;

import lombok.Data;

@Data
public class WarningItemVO {
    private String riskLevel;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String problem;

    private String reason;

    private String suggestOwner;

    private String suggestAction;
}
