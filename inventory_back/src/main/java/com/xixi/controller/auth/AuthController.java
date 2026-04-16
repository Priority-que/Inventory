package com.xixi.controller.auth;

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
public class AuthController {
    private final AuthService authService;
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        return authService.logout(request);
    }

    @GetMapping("/me")
    public Result me() {
        return authService.me();
    }

    @PutMapping("/password")
    public Result changePassword(@RequestBody ChangePasswordDTO changePasswordDTO,
                                 HttpServletRequest request) {
        return authService.changePassword(changePasswordDTO, request);
    }
}
