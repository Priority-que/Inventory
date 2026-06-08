package com.xixi.pojo.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(name = "InventoryPageVO", description = "Inventory page response")
public class InventoryPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Inventory id")
    private Long id;

    @Schema(description = "Material id")
    private Long materialId;

    @Schema(description = "Material code")
    private String materialCode;

    @Schema(description = "Material name")
    private String materialName;

    @Schema(description = "Specification")
    private String specification;

    @Schema(description = "Unit")
    private String unit;

    @Schema(description = "Warehouse id")
    private Long warehouseId;

    @Schema(description = "Warehouse name")
    private String warehouseName;

    @Schema(description = "Current stock number")
    private BigDecimal currentNumber;

    @Schema(description = "Safety stock number")
    private BigDecimal safetyNumber;

    @Schema(description = "Upper stock number")
    private BigDecimal upperNumber;

    @Schema(description = "Stock status: NORMAL, LOW, OVER")
    private String stockStatus;

    @Schema(description = "Last inbound time")
    private LocalDateTime lastInboundTime;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}