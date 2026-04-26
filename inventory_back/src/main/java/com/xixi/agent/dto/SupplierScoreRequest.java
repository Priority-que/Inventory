package com.xixi.agent.dto;

import lombok.Data;

@Data
public class SupplierScoreRequest {
    private Long supplierId;

    private Integer days = 30;

    private String threadId;
}
