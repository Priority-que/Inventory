package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.user.UserDTO;
import com.xixi.pojo.dto.user.UserRoleDTO;
import com.xixi.pojo.dto.user.UserStatusDTO;
import com.xixi.pojo.query.user.UserQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.user.UserVO;

public interface UserService {
    IPage<UserVO> getUserPage(UserQuery userQuery);

    UserVO getUserDetailById(Long id);

    Result addUser(UserDTO userDTO);

    Result updateUser(UserDTO userDTO);

    Result updateUserStatus(UserStatusDTO userStatusDTO);

    Result resetPassword(Long id);

    Result updateUserRole(UserRoleDTO userRoleDTO);
}
