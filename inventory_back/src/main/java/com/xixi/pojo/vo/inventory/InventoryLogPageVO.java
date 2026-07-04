package com.xixi.pojo.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(name = "InventoryLogPageVO", description = "库存流水分页响应")
public class InventoryLogPageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String logNo;
    private Long inventoryId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long warehouseId;
    private String warehouseName;
    private String bizType;
    private Long bizId;
    private BigDecimal beforeNumber;
    private BigDecimal changeNumber;
    private BigDecimal afterNumber;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime operateTime;
}
