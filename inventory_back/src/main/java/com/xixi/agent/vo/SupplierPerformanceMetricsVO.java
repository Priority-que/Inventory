package com.xixi.agent.vo;

import lombok.Data;

@Data
public class SupplierPerformanceMetricsVO {
    private Long supplierId;

    private String supplierName;

    private Integer totalOrderCount;

    private Integer completedOrderCount;

    private Integer cancelledOrderCount;

    private Integer abnormalArrivalCount;

    private Integer totalArrivalCount;

    private Double confirmRate;

    private Double arrivalCompletionRate;

    private Double inboundCompletionRate;

    private Double abnormalArrivalRate;
}
