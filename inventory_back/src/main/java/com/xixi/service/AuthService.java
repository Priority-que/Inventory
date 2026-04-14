package com.xixi.service;

import com.xixi.pojo.dto.auth.ChangePasswordDTO;
import com.xixi.pojo.dto.auth.LoginDTO;
import com.xixi.pojo.vo.Result;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    Result login(LoginDTO loginDTO);
    Result logout(HttpServletRequest request);
    Result me();
    Result changePassword(ChangePasswordDTO changePasswordDTO,HttpServletRequest request);
}
