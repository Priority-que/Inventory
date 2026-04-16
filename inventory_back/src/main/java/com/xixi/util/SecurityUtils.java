package com.xixi.util;

import com.xixi.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class SecurityUtils {
    public static LoginUser getCurrentLoginUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null){
            return null;
        }
        Object principal = authentication.getPrincipal();
        if(principal instanceof LoginUser){
            return (LoginUser) principal;
        }
        return null;
    }
    public static Long getCurrentUserId(){
        LoginUser loginUser = getCurrentLoginUser();
        if(loginUser == null){
            return null;
        }
        return loginUser.getUserId();
    }
    public static String getCurrentUsername(){
        LoginUser loginUser = getCurrentLoginUser();
        if(loginUser == null){
            return null;
        }
        return loginUser.getUsername();
    }
    public static List<String> getCurrentUserRoleCodes(){
        LoginUser loginUser = getCurrentLoginUser();
        List<String> list = loginUser.getRoleCodes();
        if(list == null){
            return null;
        }
        return list;
    }
}
