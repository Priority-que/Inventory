package com.xixi.pojo.vo.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(name = "PurchaseOrderVO", description = "PurchaseOrderVO")
public class PurchaseOrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "采购订单主键ID")
    private Long id;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "采购申请ID")
    private Long requestId;

    @Schema(description = "采购申请标题")
    private String requestTitle;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "采购员ID")
    private Long purchaserId;

    @Schema(description = "采购员姓名")
    private String purchaserName;

    @Schema(description = "计划交期")
    private LocalDate planDate;

    @Schema(description = "供应商反馈交期")
    private LocalDate supplierDate;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "采购订单状态，WAIT_CONFIRM待供应商确认，IN_PROGRESS履约中，PARTIAL_ARRIVAL部分到货，COMPLETED已完成，CLOSED已关闭，CANCELLED已取消")
    private String status;

    @Schema(description = "供应商备注")
    private String supplierNote;

    @Schema(description = "关闭时间")
    private LocalDateTime closeTime;

    @Schema(description = "关闭原因")
    private String closeReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除标识，0未删除，1已删除")
    private Integer deleted;
}

