package com.xixi.pojo.vo.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OperLogPageVO implements Serializable {
    private  static  final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private  Long id;

    private  String logType;

    private  String moduleName;

    private  String bizType;

    private  Long bizId;

    private  String operationType;

    private  String operationDesc;

    private  Long operatorId;

    private  String operatorName;

    private String requestUri;

    private  String requestMethod;;

    private String ipAddress;

    private  Integer successFlag;

    private String errorMessage;

    private LocalDateTime operateTime;
}
