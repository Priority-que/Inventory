package com.xixi.pojo.query.log;

import lombok.Data;

@Data
public class OperLogPageQuery {
    private Integer pageNum=1;

    private  Integer pageSize=10;
    private String logType;

    private String operatorName;

    private  String moduleName;

    private  String operationType;

    private  Long operatorId;

    private Integer successFlag;

    private String beginTime;

    private String endTime;

}
