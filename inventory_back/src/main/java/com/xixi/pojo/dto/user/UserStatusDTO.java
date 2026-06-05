package com.xixi.pojo.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "UserStatusDTO", description = "UserStatusDTO")
public class UserStatusDTO {
    @Schema(description = "用户状态主键ID")
    private Long id;
    @Schema(description = "用户状态，ENABLED启用，DISABLED禁用")
    private String status;
}

