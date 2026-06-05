package com.xixi.pojo.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "PurchaseOrderItemDTO", description = "PurchaseOrderItemDTO")
public class PurchaseOrderItemDTO {
    @Schema(description = "采购订单明细主键ID")
    private Long id;
    @Schema(description = "单价")
    private BigDecimal unitPrice;
    @Schema(description = "备注")
    private String remark;
}

