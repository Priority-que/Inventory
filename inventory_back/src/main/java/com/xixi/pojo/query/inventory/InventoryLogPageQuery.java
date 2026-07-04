package com.xixi.pojo.query.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "InventoryLogPageQuery", description = "库存流水分页查询参数")
public class InventoryLogPageQuery {

    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "库存台账ID")
    private Long inventoryId;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "业务类型，INBOUND入库，ADJUST调整")
    private String bizType;

    @Schema(description = "操作开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "操作结束时间")
    private LocalDateTime endTime;
}
