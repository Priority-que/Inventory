package com.xixi.pojo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "LoginDTO", description = "LoginDTO")
public class LoginDTO {
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
}

