package com.xixi.pojo.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserVO {
    private Long id;
    private String username;
    private String name;
    private String dept;
    private String status;
    private List<String> roleCodes;
}
