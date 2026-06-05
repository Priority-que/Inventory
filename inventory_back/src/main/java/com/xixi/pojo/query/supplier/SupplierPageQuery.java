package com.xixi.pojo.query.supplier;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "SupplierPageQuery", description = "SupplierPageQuery")
public class SupplierPageQuery {
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "供应商编码")
    private String code;

    @Schema(description = "供应商名称")
    private  String name;

    @Schema(description = "联系人姓名")
    private  String contactName;

    @Schema(description = "联系人电话")
    private  String contactPhone;

    @Schema(description = "供应商状态，PENDING待审核，REJECTED驳回，ACTIVE启用，DISABLED停用")
    private String status;
}

