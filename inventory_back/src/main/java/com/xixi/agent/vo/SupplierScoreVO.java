package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierScoreVO {
    private Long supplierId;

    private String supplierName;

    private Integer score;

    private String level;

    private String confirmRate;

    private String arrivalCompletionRate;

    private String inboundCompletionRate;

    private String abnormalArrivalRate;

    private String analysis;

    private String suggestion;
}
