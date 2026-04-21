package com.xixi.common;

import com.xixi.pojo.vo.Result;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public Result handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException e) {
        return Result.error(401, e.getMessage() == null ? "请先登录" : e.getMessage());
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            UsernameNotFoundException.class,
            AuthenticationException.class
    })
    public Result handleAuthenticationException(Exception e) {
        return Result.error(401, "用户名或密码错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result handleAccessDeniedException(AccessDeniedException e) {
        return Result.error(403, e.getMessage() == null ? "无权访问该资源" : e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleMaxUploadSizeExceededException(Exception e) {
        return Result.error("上传文件不能超过10MB！");
    }

    @ExceptionHandler(MultipartException.class)
    public Result handleMultipartException(MultipartException e){
        return Result.error("上传文件超过限制，请重新选择文件！");
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        return Result.error(e.getMessage() == null ? "系统异常，请联系管理员！" : e.getMessage());
    }

}
