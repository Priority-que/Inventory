package com.xixi.service.impl;

import com.xixi.annotation.OperLogRecord;
import com.xixi.config.JwtProperties;
import com.xixi.entity.User;
import com.xixi.mapper.UserMapper;
import com.xixi.pojo.dto.auth.ChangePasswordDTO;
import com.xixi.pojo.dto.auth.LoginDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.auth.CurrentUserVO;
import com.xixi.pojo.vo.auth.LoginVO;
import com.xixi.security.JwtTokenService;
import com.xixi.security.LoginUser;
import com.xixi.service.AuthService;
import com.xixi.service.TokenBlacklistService;
import com.xixi.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @OperLogRecord(
            logType = "LOGIN",
            moduleName = "认证管理",
            operationType = "LOGIN",
            operationDesc = "用户登录",
            bizType = "USER"
    )
    public Result login(LoginDTO loginDTO) {
        if (loginDTO.getUsername() == null || loginDTO.getUsername().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().isEmpty()) {
            return Result.error("密码不能为空");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(loginUser);
        userMapper.updateLastLoginTimeAndCreateTime(loginUser.getUserId(), LocalDateTime.now());
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setTokenType(jwtProperties.getPrefix());
        loginVO.setExpiresIn(jwtProperties.getExpireMinutes() * 60);
        loginVO.setUserId(loginUser.getUserId());
        loginVO.setUsername(loginUser.getUsername());
        loginVO.setName(loginUser.getName());
        loginVO.setRoleCodes(loginUser.getRoleCodes());
        return Result.success(loginVO);
    }

    @Override
    @OperLogRecord(
            logType = "LOGOUT",
            moduleName = "认证管理",
            operationType = "LOGOUT",
            operationDesc = "用户退出登录",
            bizType = "USER"
    )
    public Result logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未登录或登录已失效");
        }
        if (!jwtTokenService.validateToken(token)) {
            return Result.error(401, "未登录或登录已失效");
        }
        String jti = jwtTokenService.getJti(token);
        long remainingMillis = jwtTokenService.getRemainingMillis(token);
        if (jti != null && remainingMillis > 0) {
            tokenBlacklistService.blacklist(jti, remainingMillis);
        }
        return Result.success("退出登录成功");
    }

    @Override
    public Result me() {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null) {
            return Result.error(401, "未登录或登录已失效");
        }
        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setId(loginUser.getUserId());
        currentUserVO.setUsername(loginUser.getUsername());
        currentUserVO.setName(loginUser.getName());
        currentUserVO.setDept(loginUser.getDept());
        currentUserVO.setStatus(loginUser.getStatus());
        currentUserVO.setRoleCodes(loginUser.getRoleCodes());
        return Result.success(currentUserVO);
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "认证管理",
            operationType = "UPDATE",
            operationDesc = "修改密码",
            bizType = "USER"
    )
    public Result changePassword(ChangePasswordDTO changePasswordDTO, HttpServletRequest request) {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null) {
            return Result.error(401, "未登录或登录已失效");
        }
        if (changePasswordDTO.getOldPassword() == null || changePasswordDTO.getOldPassword().isEmpty()) {
            return Result.error("旧密码不能为空");
        }
        if (changePasswordDTO.getNewPassword() == null || changePasswordDTO.getNewPassword().isEmpty()) {
            return Result.error("新密码不能为空");
        }
        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            return Result.error("新密码和确认密码不一致");
        }
        User user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            return Result.error("旧密码错误");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        if (userMapper.updateById(updateUser) <= 0) {
            return Result.error("修改密码失败");
        }
        String token = getTokenFromRequest(request);
        if (token != null && jwtTokenService.validateToken(token)) {
            String jti = jwtTokenService.getJti(token);
            long remainingMillis = jwtTokenService.getRemainingMillis(token);
            if (jti != null && remainingMillis > 0) {
                tokenBlacklistService.blacklist(jti, remainingMillis);
            }
        }
        return Result.success("修改密码成功");
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (header == null || !header.startsWith(jwtProperties.getPrefix() + " ")) {
            return null;
        }
        return header.substring((jwtProperties.getPrefix() + " ").length());
    }
}
