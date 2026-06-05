package com.xixi.pojo.query.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "PurchaseRequestQuery", description = "PurchaseRequestQuery")
public class PurchaseRequestQuery {
    @Schema(description = "当前页码")
    private Integer pageNum=1;
    @Schema(description = "每页条数")
    private Integer pageSize=10;
    @Schema(description = "采购申请单号")
    private String requestNo;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "申请人ID")
    private Long applicantId;
    @Schema(description = "所属部门")
    private String dept;
    @Schema(description = "提交时间开始")
    private LocalDateTime submitTimeBegin;
    @Schema(description = "提交时间结束")
    private LocalDateTime submitTimeEnd;
    @Schema(description = "采购申请状态，DRAFT草稿，SUBMITTED已提交，APPROVED已通过，REJECTED已驳回，WITHDRAWN已撤回，ORDER_CREATED已生成订单")
    private String status;
}

