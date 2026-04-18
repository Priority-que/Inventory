package com.xixi.common;

import com.xixi.pojo.vo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;


@RestControllerAdvice
public class GlobalExceptionHandler {

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
