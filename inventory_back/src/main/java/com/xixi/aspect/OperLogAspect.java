package com.xixi.aspect;

import com.xixi.pojo.dto.material.MaterialDTO;
import com.xixi.pojo.dto.supplier.SupplierDTO;
import com.xixi.pojo.dto.warehouse.WarehouseDTO;
import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.OperLog;
import com.xixi.security.LoginUser;
import com.xixi.service.OperLogRecordService;
import com.xixi.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final OperLogRecordService operLogRecordService;

    @Around("@annotation(operLogRecord)")
    public Object recordOperLog(ProceedingJoinPoint joinPoint, OperLogRecord operLogRecord) throws Throwable {
        Object result=null;
        Integer successFlag=1;
        String errorMessage=null;

        String moduleName=operLogRecord.moduleName();
        String operationType=operLogRecord.operationType();
        String operationDesc=operLogRecord.operationDesc();
        String bizType=operLogRecord.bizType();
        Long bizId = getBizId(joinPoint.getArgs(), bizType);

        try{
            result=joinPoint.proceed();
            return  result;
        }catch (Throwable e){
            successFlag=0;
            errorMessage=e.getMessage();
            throw e;
        }
        finally {
            try{
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletRequest request = attributes == null ? null : attributes.getRequest();
                LoginUser loginUser= SecurityUtils.getCurrentLoginUser();

                OperLog operLog=new OperLog();
                operLog.setLogType(operLogRecord.logType());
                operLog.setModuleName(moduleName);
                operLog.setBizType(bizType==null||bizType.isBlank()?null:bizType);
                operLog.setBizId(bizId);
                operLog.setOperationType(operationType);
                operLog.setOperationDesc(operationDesc);
                operLog.setOperatorId(loginUser==null?null:loginUser.getUserId());
                operLog.setOperatorName(loginUser==null?null:loginUser.getName());
                operLog.setRequestUri(request==null?null:request.getRequestURI());
                operLog.setRequestMethod(request==null?null:request.getMethod());
                operLog.setIpAddress(request==null?null:request.getRemoteAddr());
                operLog.setSuccessFlag(successFlag);
                if(errorMessage!=null && errorMessage.length()>500){
                    errorMessage=errorMessage.substring(0,500);
                }
                operLog.setErrorMessage(errorMessage);
                operLog.setOperateTime(LocalDateTime.now());
                operLog.setCreateBy(loginUser==null?null:loginUser.getUserId());
                operLog.setCreateTime(LocalDateTime.now());

                operLogRecordService.saveOperLog(operLog);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private Long getBizId(Object[] args, String bizType){
        if(args==null || bizType==null || bizType.isBlank()){
            return null;
        }
        for (Object arg : args) {
            if("SUPPLIER".equals(bizType) && arg instanceof SupplierDTO supplierDTO){
                return  supplierDTO.getId();
            }
            if ("WAREHOUSE".equals(bizType) && arg instanceof WarehouseDTO warehouseDTO) {
                return warehouseDTO.getId();
            }
            if ("MATERIAL".equals(bizType) && arg instanceof MaterialDTO materialDTO) {
                return materialDTO.getId();
            }
        }
        return null;
    }
}
