package com.xixi.pojo.dto.arrival;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "ArrivalItemDTO", description = "ArrivalItemDTO")
public class ArrivalItemDTO {
    @Schema(description = "采购订单明细ID")
    private Long orderItemId;

    @Schema(description = "到货数量")
    private BigDecimal arrivalNumber;

    @Schema(description = "合格数量")
    private BigDecimal qualifiedNumber;

    @Schema(description = "不合格数量")
    private BigDecimal unqualifiedNumber;

    @Schema(description = "异常说明")
    private String abnormalNote;

    @Schema(description = "备注")
    private String remark;
}

