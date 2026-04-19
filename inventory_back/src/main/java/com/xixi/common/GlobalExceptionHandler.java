package com.xixi.common;

import com.xixi.pojo.vo.Result;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String,String> DUPLICATE_MESSAGE_MAP=Map.of(
            "warehouse.uk_warehouse_code", "仓库编码已存在，请更换后重试！",
            "supplier.uk_supplier_code", "供应商编码已存在，请更换后重试！"
    );

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleMaxUploadSizeExceededException(Exception e) {
        return Result.error("上传文件不能超过10MB！");
    }

    @ExceptionHandler(MultipartException.class)
    public Result handleMultipartException(MultipartException e) {
        return Result.error("上传文件超过限制，请重新选择文件！");
    }

    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public Result handleDuplicateKeyException(Exception e){
        return Result.error(getDuplicateMessage(e.getMessage()));

    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        e.printStackTrace();
        return Result.error("系统异常，请联系管理员！");
    }

    private String getDuplicateMessage(String msg){
        if(msg==null || msg.isBlank()){
            return null;
        }
        for (Map.Entry<String,String> entry : DUPLICATE_MESSAGE_MAP.entrySet()) {
            if(msg.contains(entry.getKey())){
                return entry.getValue();
            }
        }
        return "系统异常，请联系管理员！";
    }
}
