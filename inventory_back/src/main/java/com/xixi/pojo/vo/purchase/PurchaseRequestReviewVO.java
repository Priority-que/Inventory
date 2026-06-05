package com.xixi.pojo.vo.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(name = "PurchaseRequestReviewVO", description = "PurchaseRequestReviewVO")
public class PurchaseRequestReviewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "采购审批记录主键ID")
    private Long id;

    @Schema(description = "采购申请ID")
    private Long requestId;

    @Schema(description = "操作动作类型")
    private String actionType;

    @Schema(description = "原状态")
    private String fromStatus;

    @Schema(description = "新状态")
    private String toStatus;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作备注")
    private String operateNote;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}

