package com.xixi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory_log")
public class InventoryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String logNo;

    private Long inventoryId;

    private Long materialId;

    private Long warehouseId;

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
