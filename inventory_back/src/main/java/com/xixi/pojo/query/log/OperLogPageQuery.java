package com.xixi.pojo.query.log;

import lombok.Data;

@Data
public class OperLogPageQuery {
    private Integer pageNum;

    private  Integer pageSize;

    private String logType;

    private  String moduleName;

    private  String operationType;

    private  Long operatorId;

    private Integer successFlag;

    private String beginTime;

    private String endTime;

}
