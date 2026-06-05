package com.xixi.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.pojo.dto.auth.ChangePasswordDTO;
import com.xixi.pojo.dto.auth.LoginDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "认证管理接口")
public class AuthController {
    private final AuthService authService;
    @Operation(summary = "用户登录", operationId = "login")
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }
    @Operation(summary = "用户登出", operationId = "logout")
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        return authService.logout(request);
    }

    @Operation(summary = "获取当前用户", operationId = "me")
    @GetMapping("/me")
    public Result me() {
        return authService.me();
    }

    @Operation(summary = "修改当前用户密码", operationId = "changePassword")
    @PutMapping("/password")
    public Result changePassword(@RequestBody ChangePasswordDTO changePasswordDTO,
                                 HttpServletRequest request) {
        return authService.changePassword(changePasswordDTO, request);
    }
}

