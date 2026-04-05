package com.xixi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("arrival")
public class Arrival implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String arrivalNo;

    private Long orderId;

    private Long warehouseId;

    private LocalDate arrivalDate;

    private BigDecimal arrivalNumber;

    private BigDecimal qualifiedNumber;

    private BigDecimal unqualifiedNumber;

    private String status;

    private String abnormalNote;

    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
