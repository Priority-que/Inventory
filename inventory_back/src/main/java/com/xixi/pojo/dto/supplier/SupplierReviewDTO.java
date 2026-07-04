package com.xixi.pojo.dto.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "SupplierReviewDTO", description = "供应商审核操作参数")
public class SupplierReviewDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "供应商ID")
    private Long id;

    @Schema(description = "审核意见或操作原因")
    private String reviewNote;
}
