package com.xixi.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "OrderDiagnosisRequest", description = "OrderDiagnosisRequest")
public class OrderDiagnosisRequest {
    @Schema(description = "采购订单号")
    private String orderNo;
    @Schema(description = "会话线程ID")
    private String threadId;
}

