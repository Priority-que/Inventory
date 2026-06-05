package com.xixi.pojo.query.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(name = "PurchaseOrderQuery", description = "PurchaseOrderQuery")
public class PurchaseOrderQuery {
    @Schema(description = "当前页码")
    private Integer pageNum=1;
    @Schema(description = "每页条数")
    private Integer pageSize=10;
    @Schema(description = "采购订单号")
    private String orderNo;
    @Schema(description = "采购申请标题")
    private String requestTitle;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "采购名称")
    private String purchaseName;
    @Schema(description = "计划交期开始")
    private LocalDate planDateBegin;
    @Schema(description = "计划交期结束")
    private LocalDate planDateEnd;
    @Schema(description = "采购订单状态，WAIT_CONFIRM待供应商确认，IN_PROGRESS履约中，PARTIAL_ARRIVAL部分到货，COMPLETED已完成，CLOSED已关闭，CANCELLED已取消")
    private String status;
}

