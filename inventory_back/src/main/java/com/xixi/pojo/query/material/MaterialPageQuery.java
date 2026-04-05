package com.xixi.pojo.query.material;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialPageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String code;
    private String name;
    private String specification;
    private BigDecimal safetyNumber;
    private BigDecimal safetyNumberBegin;
    private BigDecimal upperNumber;
    private BigDecimal upperNumberBegin;
    private String status;
}
