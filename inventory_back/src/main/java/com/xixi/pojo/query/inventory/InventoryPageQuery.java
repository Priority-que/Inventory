package com.xixi.pojo.query.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "InventoryPageQuery", description = "Inventory page query")
public class InventoryPageQuery {

    @Schema(description = "Current page number")
    private Integer pageNum = 1;

    @Schema(description = "Page size")
    private Integer pageSize = 10;

    @Schema(description = "Material code")
    private String materialCode;

    @Schema(description = "Material name")
    private String materialName;

    @Schema(description = "Warehouse name")
    private String warehouseName;

    @Schema(description = "Stock status: NORMAL, LOW, OVER")
    private String stockStatus;
}