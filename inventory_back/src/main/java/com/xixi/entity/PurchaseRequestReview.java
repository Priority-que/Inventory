package com.xixi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("purchase_request_review")
public class PurchaseRequestReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long requestId;

    private String actionType;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String operatorName;

    private String operateNote;

    private LocalDateTime operateTime;
}
