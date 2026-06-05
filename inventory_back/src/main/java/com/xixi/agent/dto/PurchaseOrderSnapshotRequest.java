package com.xixi.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "PurchaseOrderSnapshotRequest", description = "PurchaseOrderSnapshotRequest")
public class PurchaseOrderSnapshotRequest {
    @Schema(description = "采购订单号")
    private String orderNo;
}

