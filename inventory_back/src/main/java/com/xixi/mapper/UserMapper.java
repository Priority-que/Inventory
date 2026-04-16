package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.User;
import com.xixi.pojo.query.user.UserQuery;
import com.xixi.pojo.vo.user.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    IPage<UserVO> getUserPage(IPage<UserVO> userPage,@Param("q") UserQuery userQuery);

    UserVO getUserDetailById(Long id);
    @Select("select * from user where username = #{username}")
    User getUserDetailByUserName(String username);
    User getAuthUserByUsername(String username);
    void updateLastLoginTimeAndCreateTime(@Param("id") Long id,@Param("lastLoginTime") LocalDateTime lastLoginTime);
}
