package com.xixi.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.user.UserDTO;
import com.xixi.pojo.dto.user.UserRoleDTO;
import com.xixi.pojo.dto.user.UserStatusDTO;
import com.xixi.pojo.query.user.UserQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.user.UserVO;
import com.xixi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/getUserPage")
    public Result getUserPage(UserQuery userQuery) {
        IPage<UserVO> userPage = userService.getUserPage(userQuery);
        return Result.success(userPage);
    }

    @GetMapping("/getUserDetailById/{id}")
    public Result getUserDetailById(@PathVariable Long id) {
        UserVO userDetailById = userService.getUserDetailById(id);
        return Result.success(userDetailById);
    }

    @PostMapping("/addUser")
    public Result addUser(@RequestBody UserDTO userDTO) {
        return userService.addUser(userDTO);
    }

    @PutMapping("/updateUser")
    public Result updateUser(@RequestBody UserDTO userDTO) {
        return userService.updateUser(userDTO);
    }

    @PutMapping("/updateUserStatus")
    public Result updateUserStatus(@RequestBody UserStatusDTO userStatusDTO) {
        return userService.updateUserStatus(userStatusDTO);
    }

    @PutMapping("/resetPassword/{id}")
    public Result resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }

    @PutMapping("/updateUserRole")
    public Result updateUserRole(@RequestBody UserRoleDTO userRoleDTO) {
        return userService.updateUserRole(userRoleDTO);
    }
}
