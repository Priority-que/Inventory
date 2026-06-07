package com.xixi.pojo.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "StatisticsSummaryVO", description = "Statistics summary response")
public class StatisticsSummaryVO {

    @Schema(description = "Pending approval purchase request count")
    private Long pendingApprovalRequestCount;

    @Schema(description = "Waiting supplier confirmation order count")
    private Long waitConfirmOrderCount;

    @Schema(description = "In progress purchase order count")
    private Long inProgressOrderCount;

    @Schema(description = "Pending inbound arrival count")
    private Long pendingInboundArrivalCount;

    @Schema(description = "Total inventory alert count")
    private Long inventoryAlertCount;

    @Schema(description = "Low stock alert count")
    private Long lowStockCount;

    @Schema(description = "Over stock alert count")
    private Long overStockCount;

    @Schema(description = "Abnormal arrival count")
    private Long abnormalArrivalCount;
}
