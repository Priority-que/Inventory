package com.xixi.pojo.query.log;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "OperLogPageQuery", description = "OperLogPageQuery")
public class OperLogPageQuery {
    @Schema(description = "当前页码")
    private Integer pageNum=1;

    @Schema(description = "每页条数")
    private  Integer pageSize=10;
    @Schema(description = "日志类型")
    private String logType;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "模块名称")
    private  String moduleName;

    @Schema(description = "操作类型")
    private  String operationType;

    @Schema(description = "操作人ID")
    private  Long operatorId;

    @Schema(description = "是否成功")
    private Integer successFlag;

    @Schema(description = "开始时间")
    private String beginTime;

    @Schema(description = "结束时间")
    private String endTime;

}

