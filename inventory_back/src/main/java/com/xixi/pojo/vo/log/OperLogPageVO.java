package com.xixi.pojo.vo.log;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(name = "OperLogPageVO", description = "OperLogPageVO")
public class OperLogPageVO implements Serializable {
    private  static  final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "操作日志主键ID")
    private  Long id;

    @Schema(description = "日志类型")
    private  String logType;

    @Schema(description = "模块名称")
    private  String moduleName;

    @Schema(description = "业务类型编码")
    private  String bizType;

    @Schema(description = "业务单据ID")
    private  Long bizId;

    @Schema(description = "操作类型")
    private  String operationType;

    @Schema(description = "操作描述")
    private  String operationDesc;

    @Schema(description = "操作人ID")
    private  Long operatorId;

    @Schema(description = "操作人姓名")
    private  String operatorName;

    @Schema(description = "请求路径")
    private String requestUri;

    @Schema(description = "请求方法")
    private  String requestMethod;;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "是否成功")
    private  Integer successFlag;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}

