package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SupplierScoreVO", description = "SupplierScoreVO")
public class SupplierScoreVO {
    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "评分信息")
    private Integer score;

    @Schema(description = "等级")
    private String level;

    @Schema(description = "确认率")
    private String confirmRate;

    @Schema(description = "到货完成率")
    private String arrivalCompletionRate;

    @Schema(description = "入库完成率")
    private String inboundCompletionRate;

    @Schema(description = "异常到货率")
    private String abnormalArrivalRate;

    @Schema(description = "分析内容")
    private String analysis;

    @Schema(description = "建议")
    private String suggestion;
}

