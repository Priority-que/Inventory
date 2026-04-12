package com.xixi.pojo.query.supplier;

import lombok.Data;

@Data
public class SupplierPageQuery {
    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String code;

    private  String name;

    private  String contactName;

    private  String contactPhone;

    private String status;
}
