package com.xixi.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "SupplierScoreRequest", description = "SupplierScoreRequest")
public class SupplierScoreRequest {
    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "统计天数")
    private Integer days = 30;

    @Schema(description = "会话线程ID")
    private String threadId;
}

