package com.xixi.pojo.vo.purchase;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
public class PurchaseRequestPageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestNo;

    private String title;

    private Long applicantId;

    private String applicantName;

    private String dept;

    private LocalDate expectedDate;

    private LocalDateTime submitTime;

    private Long reviewUserId;

    private String reviewUserName;

    private LocalDateTime reviewTime;

    private String reviewNote;

    private String status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
