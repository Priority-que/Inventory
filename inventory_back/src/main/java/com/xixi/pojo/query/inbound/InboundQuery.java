package com.xixi.pojo.query.inbound;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InboundQuery {
    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String inboundNo;

    private String arrivalNo;

    private String orderNo;

    private String warehouseName;

    private String status;

    private LocalDateTime inboundTimeBegin;

    private LocalDateTime inboundTimeEnd;
}
