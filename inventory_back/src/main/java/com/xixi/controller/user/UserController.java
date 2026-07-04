package com.xixi.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.user.UserDTO;
import com.xixi.pojo.dto.user.UserRoleDTO;
import com.xixi.pojo.dto.user.UserStatusDTO;
import com.xixi.pojo.query.user.UserQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.user.UserVO;
import com.xixi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理接口")
public class UserController {
    private final UserService userService;

    @Operation(summary = "分页查询用户", operationId = "getUserPage")
    @GetMapping("/getUserPage")
    @PreAuthorize("hasRole('ADMIN')")
    public Result getUserPage(UserQuery userQuery) {
        IPage<UserVO> userPage = userService.getUserPage(userQuery);
        return Result.success(userPage);
    }

    @Operation(summary = "查询用户详情", operationId = "getUserDetailById")
    @GetMapping("/getUserDetailById/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','PURCHASE_MANAGER','WAREHOUSE','SUPPLIER')")
    public Result getUserDetailById(@PathVariable Long id) {
        UserVO userDetailById = userService.getUserDetailById(id);
        return Result.success(userDetailById);
    }

    @Operation(summary = "新增用户", operationId = "addUser")
    @PostMapping("/addUser")
    @PreAuthorize("hasRole('ADMIN')")
    public Result addUser(@RequestBody UserDTO userDTO) {
        return userService.addUser(userDTO);
    }

    @Operation(summary = "更新用户", operationId = "updateUser")
    @PutMapping("/updateUser")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','PURCHASE_MANAGER','WAREHOUSE','SUPPLIER')")
    public Result updateUser(@RequestBody UserDTO userDTO) {
        return userService.updateUser(userDTO);
    }

    @Operation(summary = "更新用户状态", operationId = "updateUserStatus")
    @PutMapping("/updateUserStatus")
    @PreAuthorize("hasRole('ADMIN')")
    public Result updateUserStatus(@RequestBody UserStatusDTO userStatusDTO) {
        return userService.updateUserStatus(userStatusDTO);
    }

    @Operation(summary = "重置用户密码", operationId = "resetPassword")
    @PutMapping("/resetPassword/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }

    @Operation(summary = "更新用户角色", operationId = "updateUserRole")
    @PutMapping("/updateUserRole")
    @PreAuthorize("hasRole('ADMIN')")
    public Result updateUserRole(@RequestBody UserRoleDTO userRoleDTO) {
        return userService.updateUserRole(userRoleDTO);
    }
}

