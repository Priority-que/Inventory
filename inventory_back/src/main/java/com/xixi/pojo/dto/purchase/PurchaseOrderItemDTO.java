package com.xixi.pojo.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemDTO {
    private Long id;
    private BigDecimal unitPrice;
    private String remark;
}
