package com.xixi.pojo.query.user;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "UserQuery", description = "UserQuery")
public class UserQuery {
    @Schema(description = "当前页码")
    private Integer pageNum=1;
    @Schema(description = "每页条数")
    private Integer pageSize=10;
    @Schema(description = "用户姓名")
    private String name;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "所属部门")
    private String dept;
    @Schema(description = "用户状态，ENABLED启用，DISABLED禁用")
    private String status;
}

