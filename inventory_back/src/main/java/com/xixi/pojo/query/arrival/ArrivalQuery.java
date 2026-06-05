package com.xixi.pojo.query.arrival;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(name = "ArrivalQuery", description = "ArrivalQuery")
public class ArrivalQuery {
    @Schema(description = "当前页码")
    private Integer pageNum=1;
    @Schema(description = "每页条数")
    private Integer pageSize=10;
    @Schema(description = "到货单号")
    private String arrivalNo;
    @Schema(description = "采购订单号")
    private String orderNo;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "到货日期开始")
    private LocalDate arrivalDateBegin;
    @Schema(description = "到货日期结束")
    private LocalDate arrivalDateEnd;
    @Schema(description = "到货状态，NORMAL正常到货，ABNORMAL异常到货")
    private String status;

    @Schema(description = "是否只查询未生成有效入库单的到货单")
    private Boolean pendingInboundOnly;
}

