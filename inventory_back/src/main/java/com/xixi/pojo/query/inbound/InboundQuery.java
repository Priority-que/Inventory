package com.xixi.pojo.query.inbound;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "InboundQuery", description = "InboundQuery")
public class InboundQuery {
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "入库单号")
    private String inboundNo;

    @Schema(description = "到货单号")
    private String arrivalNo;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "入库状态，PENDING待确认，COMPLETED已完成，CANCELLED已取消，ABNORMAL异常")
    private String status;

    @Schema(description = "入库开始时间")
    private LocalDateTime inboundTimeBegin;

    @Schema(description = "入库结束时间")
    private LocalDateTime inboundTimeEnd;
}

