package com.xixi.pojo.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "UserRoleDTO", description = "UserRoleDTO")
public class UserRoleDTO {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "角色ID")
    private Long roleId;
}

