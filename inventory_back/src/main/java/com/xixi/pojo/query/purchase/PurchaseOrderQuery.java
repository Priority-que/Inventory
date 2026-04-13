package com.xixi.pojo.query.purchase;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PurchaseOrderQuery {
    private Integer pageNum=1;
    private Integer pageSize=10;
    private String orderNo;
    private String requestTitle;
    private String supplierName;
    private String purchaseName;
    private LocalDate planDateBegin;
    private LocalDate planDateEnd;
    private String status;
}
