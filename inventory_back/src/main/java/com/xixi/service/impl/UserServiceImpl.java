package com.xixi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.Role;
import com.xixi.entity.User;
import com.xixi.entity.UserRole;
import com.xixi.mapper.RoleMapper;
import com.xixi.mapper.UserMapper;
import com.xixi.mapper.UserRoleMapper;
import com.xixi.pojo.dto.user.UserDTO;
import com.xixi.pojo.dto.user.UserRoleDTO;
import com.xixi.pojo.dto.user.UserStatusDTO;
import com.xixi.pojo.query.user.UserQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.user.UserVO;
import com.xixi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public IPage<UserVO> getUserPage(UserQuery userQuery) {
        IPage<UserVO> userPage = new Page<>(userQuery.getPageNum(), userQuery.getPageSize());
        return userMapper.getUserPage(userPage, userQuery);
    }

    @Override
    public UserVO getUserDetailById(Long id) {
        return userMapper.getUserDetailById(id);
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "用户管理",
            operationType = "CREATE",
            operationDesc = "新增用户",
            bizType = "USER"
    )
    public Result addUser(UserDTO userDTO) {
        if (userDTO.getUsername() == null) {
            return Result.error("用户名不能为空");
        }
        if (userDTO.getName() == null) {
            return Result.error("用户姓名不能为空");
        }
        if (userDTO.getRoleId() == null) {
            return Result.error("用户角色不能为空");
        }
        String username = userDTO.getUsername();
        User existingUser = userMapper.getUserDetailByUserName(username);
        if (existingUser != null) {
            return Result.error("用户名已存在");
        }
        Role role = roleMapper.selectById(userDTO.getRoleId());
        if (role == null) {
            return Result.error("角色不存在");
        }
        if ("DISABLED".equals(role.getStatus())) {
            return Result.error("角色已禁用");
        }

        User user = new User();
        user.setUsername(username);
        user.setName(userDTO.getName());
        user.setPassword(passwordEncoder.encode("123456"));
        user.setStatus("ENABLED");
        user.setDept(userDTO.getDept());
        user.setPhone(userDTO.getPhone());
        user.setEmail(userDTO.getEmail());
        user.setRemark(userDTO.getRemark());
        if (userMapper.insert(user) <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("添加用户失败");
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(userDTO.getRoleId());
        userRole.setIsPrimary(1);
        if (userRoleMapper.insert(userRole) > 0) {
            userDTO.setId(user.getId());
            return Result.success("添加用户成功");
        }
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return Result.error("添加用户失败");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "用户管理",
            operationType = "UPDATE",
            operationDesc = "修改用户",
            bizType = "USER"
    )
    public Result updateUser(UserDTO userDTO) {
        if (userDTO.getId() == null) {
            return Result.error("用户ID不能为空");
        }
        User user = new User();
        user.setId(userDTO.getId());
        user.setName(userDTO.getName());
        user.setDept(userDTO.getDept());
        user.setPhone(userDTO.getPhone());
        user.setEmail(userDTO.getEmail());
        user.setRemark(userDTO.getRemark());
        if (userMapper.updateById(user) > 0) {
            return Result.success("修改用户成功");
        }
        return Result.error("修改用户失败");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "用户管理",
            operationType = "UPDATE_STATUS",
            operationDesc = "修改用户状态",
            bizType = "USER"
    )
    public Result updateUserStatus(UserStatusDTO userStatusDTO) {
        if (userStatusDTO.getId() == null) {
            return Result.error("用户ID不能为空");
        }
        if (userStatusDTO.getStatus() == null) {
            return Result.error("用户状态不能为空");
        }
        if (!"ENABLED".equals(userStatusDTO.getStatus()) && !"DISABLED".equals(userStatusDTO.getStatus())) {
            return Result.error("用户状态非法");
        }
        User user = userMapper.selectById(userStatusDTO.getId());
        if (user == null) {
            return Result.error("用户不存在");
        }
        User updateUser = new User();
        updateUser.setId(userStatusDTO.getId());
        updateUser.setStatus(userStatusDTO.getStatus());
        if (userMapper.updateById(updateUser) > 0) {
            return Result.success("修改用户状态成功");
        }
        return Result.error("修改用户状态失败");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "用户管理",
            operationType = "RESET_PASSWORD",
            operationDesc = "重置用户密码",
            bizType = "USER"
    )
    public Result resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        if (userMapper.updateById(user) > 0) {
            return Result.success("重置密码成功");
        }
        return Result.error("重置密码失败");
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "用户管理",
            operationType = "UPDATE_ROLE",
            operationDesc = "修改用户角色",
            bizType = "USER"
    )
    public Result updateUserRole(UserRoleDTO userRoleDTO) {
        if (userRoleDTO.getUserId() == null) {
            return Result.error("用户ID不能为空");
        }
        if (userRoleDTO.getRoleId() == null) {
            return Result.error("角色ID不能为空");
        }
        User user = userMapper.selectById(userRoleDTO.getUserId());
        if (user == null) {
            return Result.error("用户不存在");
        }
        Role role = roleMapper.selectById(userRoleDTO.getRoleId());
        if (role == null) {
            return Result.error("角色不存在");
        }
        if ("DISABLED".equals(role.getStatus())) {
            return Result.error("角色已禁用");
        }

        userRoleMapper.deleteByUserId(userRoleDTO.getUserId());
        UserRole userRole = new UserRole();
        userRole.setUserId(userRoleDTO.getUserId());
        userRole.setRoleId(userRoleDTO.getRoleId());
        userRole.setIsPrimary(1);
        if (userRoleMapper.insert(userRole) > 0) {
            return Result.success("修改用户角色成功");
        }
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return Result.error("修改用户角色失败");
    }
}
