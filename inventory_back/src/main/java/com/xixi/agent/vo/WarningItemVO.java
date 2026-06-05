package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "WarningItemVO", description = "WarningItemVO")
public class WarningItemVO {
    @Schema(description = "风险等级编码")
    private String riskLevel;

    @Schema(description = "业务类型编码")
    private String bizType;

    @Schema(description = "业务单据ID")
    private Long bizId;

    @Schema(description = "业务单号")
    private String bizNo;

    @Schema(description = "问题描述")
    private String problem;

    @Schema(description = "原因说明")
    private String reason;

    @Schema(description = "建议负责人编码")
    private String suggestOwner;

    @Schema(description = "建议处理动作")
    private String suggestAction;
}

