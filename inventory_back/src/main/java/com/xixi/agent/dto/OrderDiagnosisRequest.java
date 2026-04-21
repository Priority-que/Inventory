package com.xixi.agent.dto;

import lombok.Data;

@Data
public class OrderDiagnosisRequest {
    private String orderNo;
    private String threadId;
}
