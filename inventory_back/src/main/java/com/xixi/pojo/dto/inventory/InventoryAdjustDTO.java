package com.xixi.pojo.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(name = "InventoryAdjustDTO", description = "库存调整参数")
public class InventoryAdjustDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "库存台账ID")
    private Long inventoryId;

    @Schema(description = "调整数量，可正可负")
    private BigDecimal changeNumber;

    @Schema(description = "调整原因")
    private String reason;
}
