package com.xixi.pojo.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "UserDTO", description = "UserDTO")
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "用户主键ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户姓名")
    private String name;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "所属部门")
    private String dept;

    @Schema(description = "用户状态，ENABLED启用，DISABLED禁用")
    private String status;

    @Schema(description = "备注")
    private String remark;

}

