package com.xixi.pojo.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "CurrentUserVO", description = "CurrentUserVO")
public class CurrentUserVO {
    @Schema(description = "当前用户主键ID")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户姓名")
    private String name;
    @Schema(description = "所属部门")
    private String dept;
    @Schema(description = "用户状态，ENABLED启用，DISABLED禁用")
    private String status;
    @Schema(description = "角色编码列表")
    private List<String> roleCodes;
}

