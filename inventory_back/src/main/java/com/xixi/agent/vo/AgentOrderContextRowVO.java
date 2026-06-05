package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "AgentOrderContextRowVO", description = "AgentOrderContextRowVO")
public class AgentOrderContextRowVO {
    @Schema(description = "采购订单ID")
    private Long orderId;
    @Schema(description = "采购订单号")
    private String orderNo;
    @Schema(description = "业务状态")
    private String status;
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "采购员ID")
    private Long purchaserId;
    @Schema(description = "采购员姓名")
    private String purchaserName;
    @Schema(description = "采购员部门")
    private String purchaserDept;
    @Schema(description = "采购员电话")
    private String purchaserPhone;
    @Schema(description = "采购总数量")
    private BigDecimal totalOrderNumber;
    @Schema(description = "到货总数量")
    private BigDecimal totalArriveNumber;
    @Schema(description = "入库总数量")
    private BigDecimal totalInboundNumber;
    @Schema(description = "到货次数")
    private Integer arrivalCount;
    @Schema(description = "入库次数")
    private Integer inboundCount;
}

