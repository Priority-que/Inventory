package com.xixi.pojo.query.arrival;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ArrivalQuery {
    private Integer pageNum=1;
    private Integer pageSize=10;
    private String arrivalNo;
    private String orderNo;
    private String warehouseName;
    private LocalDate arrivalDateBegin;
    private LocalDate arrivalDateEnd;
    private String status;
}
