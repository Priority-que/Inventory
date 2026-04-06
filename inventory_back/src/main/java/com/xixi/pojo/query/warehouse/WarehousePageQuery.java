package com.xixi.pojo.query.warehouse;

import lombok.Data;

@Data
public class WarehousePageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String code;
    private String name;
    private String address;
    private String managerName;
    private String status;
}
