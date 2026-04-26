package com.xixi.agent.workflow.state;

import lombok.Data;

@Data
public class WorkflowEntity {
    private String orderNo;
    private Long supplierId;
    private Integer days;
    private String materialCode;
    private Long warehouseId;
}
