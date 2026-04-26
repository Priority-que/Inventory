package com.xixi.agent.vo;

import lombok.Data;

import java.math.BigDecimal;


/*
输入订单的快照信息
* */
@Data
public class OrderSnapshotVO {
    private Long orderId;
    private String orderNo;
    private String status;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalOrderNumber;
    private BigDecimal totalArriveNumber;
    private BigDecimal totalInboundNumber;
    private Integer arrivalCount;
    private Integer inboundCount;
}
