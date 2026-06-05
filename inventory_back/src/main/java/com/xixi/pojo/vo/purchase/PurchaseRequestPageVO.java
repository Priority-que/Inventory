package com.xixi.pojo.vo.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@Schema(name = "PurchaseRequestPageVO", description = "PurchaseRequestPageVO")
public class PurchaseRequestPageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "采购申请主键ID")
    private Long id;

    @Schema(description = "采购申请单号")
    private String requestNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "申请人姓名")
    private String applicantName;

    @Schema(description = "所属部门")
    private String dept;

    @Schema(description = "期望到货日期")
    private LocalDate expectedDate;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "审批人ID")
    private Long reviewUserId;

    @Schema(description = "审批人姓名")
    private String reviewUserName;

    @Schema(description = "审批时间")
    private LocalDateTime reviewTime;

    @Schema(description = "审批意见")
    private String reviewNote;

    @Schema(description = "采购申请状态，DRAFT草稿，SUBMITTED已提交，APPROVED已通过，REJECTED已驳回，WITHDRAWN已撤回，ORDER_CREATED已生成订单")
    private String status;

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

