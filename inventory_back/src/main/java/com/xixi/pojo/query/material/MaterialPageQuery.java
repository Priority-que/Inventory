package com.xixi.pojo.query.material;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "MaterialPageQuery", description = "MaterialPageQuery")
public class MaterialPageQuery {
    @Schema(description = "当前页码")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "物料编码")
    private String code;
    @Schema(description = "物料名称")
    private String name;
    @Schema(description = "规格型号")
    private String specification;
    @Schema(description = "安全库存")
    private BigDecimal safetyNumber;
    @Schema(description = "安全库存下限")
    private BigDecimal safetyNumberBegin;
    @Schema(description = "库存上限")
    private BigDecimal upperNumber;
    @Schema(description = "库存上限下限")
    private BigDecimal upperNumberBegin;
    @Schema(description = "物料状态，ENABLED启用，DISABLED禁用")
    private String status;
}

