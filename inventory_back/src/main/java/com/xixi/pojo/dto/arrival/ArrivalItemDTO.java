package com.xixi.pojo.dto.arrival;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArrivalItemDTO {
    private Long orderItemId;

    private BigDecimal arrivalNumber;

    private BigDecimal qualifiedNumber;

    private BigDecimal unqualifiedNumber;

    private String abnormalNote;

    private String remark;
}
