package com.xixi.pojo.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "LoginVO", description = "LoginVO")
public class LoginVO {
    @Schema(description = "访问令牌")
    private String token;
    @Schema(description = "令牌类型")
    private String tokenType;
    @Schema(description = "令牌过期时间，单位秒")
    private Long expiresIn;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "登录名称")
    private String name;
    @Schema(description = "角色编码列表")
    private List<String> roleCodes;
}

