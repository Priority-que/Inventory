package com.xixi.service.impl;

import com.xixi.entity.User;
import com.xixi.mapper.RoleMapper;
import com.xixi.mapper.UserMapper;
import com.xixi.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.getAuthUserByUsername(username);
        if(user == null){
            throw new UsernameNotFoundException("用户名或者密码错误");
        }
        List<String> roleCodes = roleMapper.getRoleCodesByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_"+code)).toList();
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                user.getStatus(),
                user.getDept(),
                roleCodes,
                authorities
        );
    }
}
