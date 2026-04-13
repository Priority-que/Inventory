package com.xixi.pojo.query.purchase;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseRequestItemQuery {
    private Integer pageNum=1;
    private Integer pageSize=10;
    private String materialCode;
    private String materialName;
    private BigDecimal requestNumberBegin;
    private BigDecimal requestNumberEnd;
}
