package com.xixi.pojo.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemCreateDTO {
       private Long requestItemId;
       private BigDecimal unitPrice;
       private String remark;
       private String status;
}
