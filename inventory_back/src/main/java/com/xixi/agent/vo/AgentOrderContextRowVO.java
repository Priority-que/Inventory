package com.xixi.agent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgentOrderContextRowVO {
    private Long orderId;
    private String orderNo;
    private String status;
    private Long supplierId;
    private String supplierName;
    private Long purchaserId;
    private String purchaserName;
    private String purchaserDept;
    private String purchaserPhone;
    private BigDecimal totalOrderNumber;
    private BigDecimal totalArriveNumber;
    private BigDecimal totalInboundNumber;
    private Integer arrivalCount;
    private Integer inboundCount;
}
