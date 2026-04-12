package com.xixi.pojo.query.purchase;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PurchaseRequestQuery {
    private Integer pageNum=1;
    private Integer pageSize=10;
    private String requestNo;
    private String title;
    private String dept;
    private LocalDateTime submitTimeBegin;
    private LocalDateTime submitTimeEnd;
    private String status;
}
