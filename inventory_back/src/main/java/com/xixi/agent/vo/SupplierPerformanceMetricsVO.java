package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "SupplierPerformanceMetricsVO", description = "SupplierPerformanceMetricsVO")
public class SupplierPerformanceMetricsVO {
    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "订单总数")
    private Integer totalOrderCount;

    @Schema(description = "已完成订单数")
    private Integer completedOrderCount;

    @Schema(description = "已取消订单数")
    private Integer cancelledOrderCount;

    @Schema(description = "异常到货次数")
    private Integer abnormalArrivalCount;

    @Schema(description = "到货总次数")
    private Integer totalArrivalCount;

    @Schema(description = "确认率")
    private Double confirmRate;

    @Schema(description = "到货完成率")
    private Double arrivalCompletionRate;

    @Schema(description = "入库完成率")
    private Double inboundCompletionRate;

    @Schema(description = "异常到货率")
    private Double abnormalArrivalRate;
}

