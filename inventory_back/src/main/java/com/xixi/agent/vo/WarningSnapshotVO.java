package com.xixi.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarningSnapshotVO {
    private Long bizId;

    private String bizNo;

    private String status;

    private Long supplierId;

    private String supplierName;

    private Long warehouseId;

    private String warehouseName;

    private LocalDateTime lastOperateTime;

    private Integer overdueDays;
}
