package com.xixi.pojo.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "PurchaseOrderItemCreateDTO", description = "PurchaseOrderItemCreateDTO")
public class PurchaseOrderItemCreateDTO {
       @Schema(description = "采购申请明细ID")
       private Long requestItemId;
       @Schema(description = "单价")
       private BigDecimal unitPrice;
       @Schema(description = "备注")
       private String remark;
       @Schema(description = "采购订单状态，WAIT_CONFIRM待供应商确认，IN_PROGRESS履约中，PARTIAL_ARRIVAL部分到货，COMPLETED已完成，CLOSED已关闭，CANCELLED已取消")
       private String status;
}

