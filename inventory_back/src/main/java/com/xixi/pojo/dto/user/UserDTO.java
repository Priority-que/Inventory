package com.xixi.pojo.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;

    private String username;

    private String name;

    private String roleName;

    private Long roleId;

    private String phone;

    private String email;

    private String dept;

    private String status;

    private String remark;

}
