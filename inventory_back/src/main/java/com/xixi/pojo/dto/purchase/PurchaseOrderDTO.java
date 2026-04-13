package com.xixi.pojo.dto.purchase;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long requestId;

    private Long supplierId;

    private Long purchaserId;

    private LocalDate planDate;

    private LocalDate supplierDate;

    private LocalDateTime confirmTime;

    private BigDecimal totalAmount;

    private String status;

    private String supplierNote;

    private LocalDateTime closeTime;

    private String closeReason;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    private List<PurchaseOrderItemCreateDTO> items;
}
