package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "WarningSnapshotVO", description = "WarningSnapshotVO")
public class WarningSnapshotVO {
    @Schema(description = "业务单据ID")
    private Long bizId;

    @Schema(description = "业务单号")
    private String bizNo;

    @Schema(description = "业务状态")
    private String status;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "最后操作时间")
    private LocalDateTime lastOperateTime;

    @Schema(description = "超期天数")
    private Integer overdueDays;
}

