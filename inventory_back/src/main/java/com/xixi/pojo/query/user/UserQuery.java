package com.xixi.pojo.query.user;

import lombok.Data;

@Data
public class UserQuery {
    private Integer pageNum=1;
    private Integer pageSize=10;
    private String name;
    private String roleName;
    private String dept;
    private String status;
}
