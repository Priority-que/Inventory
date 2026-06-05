package com.xixi.pojo.query.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "WarehousePageQuery", description = "WarehousePageQuery")
public class WarehousePageQuery {
    @Schema(description = "当前页码")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "仓库编码")
    private String code;
    @Schema(description = "仓库名称")
    private String name;
    @Schema(description = "地址")
    private String address;
    @Schema(description = "仓库负责人")
    private String managerName;
    @Schema(description = "仓库状态，ENABLED启用，DISABLED禁用")
    private String status;
}

