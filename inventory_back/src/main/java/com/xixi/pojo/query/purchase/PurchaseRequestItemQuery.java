package com.xixi.pojo.query.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "PurchaseRequestItemQuery", description = "PurchaseRequestItemQuery")
public class PurchaseRequestItemQuery {
    @Schema(description = "当前页码")
    private Integer pageNum=1;
    @Schema(description = "每页条数")
    private Integer pageSize=10;
    @Schema(description = "物料编码")
    private String materialCode;
    @Schema(description = "物料名称")
    private String materialName;
    @Schema(description = "申请数量下限")
    private BigDecimal requestNumberBegin;
    @Schema(description = "申请数量上限")
    private BigDecimal requestNumberEnd;
}

