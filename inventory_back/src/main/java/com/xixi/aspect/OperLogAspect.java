package com.xixi.aspect;

import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.OperLog;
import com.xixi.pojo.dto.auth.LoginDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.auth.LoginVO;
import com.xixi.security.LoginUser;
import com.xixi.service.OperLogRecordService;
import com.xixi.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private static final int SUCCESS_CODE = 200;

    private final OperLogRecordService operLogRecordService;

    @Around("@annotation(operLogRecord)")
    public Object recordOperLog(ProceedingJoinPoint joinPoint, OperLogRecord operLogRecord) throws Throwable {
        Object result = null;
        Integer successFlag = 1;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            if (result instanceof Result resultVO && !isSuccess(resultVO)) {
                successFlag = 0;
                errorMessage = resultVO.getMsg();
            }
            return result;
        } catch (Throwable ex) {
            successFlag = 0;
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            saveOperLogs(joinPoint.getArgs(), result, operLogRecord, successFlag, errorMessage);
        }
    }

    private void saveOperLogs(Object[] args, Object result, OperLogRecord operLogRecord,
                              Integer successFlag, String errorMessage) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes == null ? null : attributes.getRequest();
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            List<Long> bizIds = extractBizIds(args, result, operLogRecord.bizType());

            if (bizIds.isEmpty()) {
                bizIds.add(null);
            }

            Long operatorId = resolveOperatorId(loginUser, result, args);
            String operatorName = resolveOperatorName(loginUser, result, args);
            LocalDateTime now = LocalDateTime.now();
            String bizType = hasText(operLogRecord.bizType()) ? operLogRecord.bizType() : null;
            String trimmedErrorMessage = trimErrorMessage(errorMessage);

            for (Long bizId : bizIds) {
                OperLog operLog = new OperLog();
                operLog.setLogType(operLogRecord.logType());
                operLog.setModuleName(operLogRecord.moduleName());
                operLog.setBizType(bizType);
                operLog.setBizId(bizId);
                operLog.setOperationType(operLogRecord.operationType());
                operLog.setOperationDesc(operLogRecord.operationDesc());
                operLog.setOperatorId(operatorId);
                operLog.setOperatorName(operatorName);
                operLog.setRequestUri(request == null ? null : request.getRequestURI());
                operLog.setRequestMethod(request == null ? null : request.getMethod());
                operLog.setIpAddress(request == null ? null : request.getRemoteAddr());
                operLog.setSuccessFlag(successFlag);
                operLog.setErrorMessage(trimmedErrorMessage);
                operLog.setOperateTime(now);
                operLog.setCreateBy(operatorId);
                operLog.setCreateTime(now);
                operLogRecordService.saveOperLog(operLog);
            }
        } catch (Exception ex) {
            log.error("Failed to save operation log", ex);
        }
    }

    private boolean isSuccess(Result result) {
        return Objects.equals(result.getCode(), SUCCESS_CODE);
    }

    private List<Long> extractBizIds(Object[] args, Object result, String bizType) {
        Set<Long> bizIds = new LinkedHashSet<>();
        Object resultData = unwrapResultData(result);
        collectBizIds(args, bizType, bizIds);
        if (bizIds.isEmpty()) {
            collectBizIds(resultData, bizType, bizIds);
        }
        return new ArrayList<>(bizIds);
    }

    private Object unwrapResultData(Object result) {
        if (result instanceof Result resultVO) {
            return resultVO.getData();
        }
        return result;
    }

    private void collectBizIds(Object source, String bizType, Set<Long> bizIds) {
        if (source == null) {
            return;
        }
        Long directId = toLong(source);
        if (directId != null) {
            bizIds.add(directId);
            return;
        }
        if (source instanceof Object[] objects) {
            for (Object object : objects) {
                collectBizIds(object, bizType, bizIds);
            }
            return;
        }
        if (source instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectBizIds(item, bizType, bizIds);
            }
            return;
        }
        if (source.getClass().isArray()) {
            int length = Array.getLength(source);
            for (int i = 0; i < length; i++) {
                collectBizIds(Array.get(source, i), bizType, bizIds);
            }
            return;
        }

        for (String fieldName : resolveBizIdFieldNames(bizType)) {
            Object fieldValue = extractFieldValue(source, fieldName);
            if (fieldValue != null) {
                collectBizIds(fieldValue, bizType, bizIds);
                if (!bizIds.isEmpty()) {
                    return;
                }
            }
        }
    }

    private List<String> resolveBizIdFieldNames(String bizType) {
        String normalizedBizType = bizType == null ? "" : bizType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedBizType) {
            case "USER" -> List.of("id", "userId");
            case "SUPPLIER" -> List.of("id", "supplierId");
            case "WAREHOUSE" -> List.of("id", "warehouseId");
            case "MATERIAL" -> List.of("id", "materialId");
            case "PURCHASE_REQUEST" -> List.of("id", "requestId");
            case "PURCHASE_REQUEST_ITEM" -> List.of("id", "requestItemId");
            case "PURCHASE_ORDER" -> List.of("id", "orderId");
            case "PURCHASE_ORDER_ITEM" -> List.of("id", "orderItemId");
            case "SUPPLIER_FILE" -> List.of("id", "fileId", "supplierId");
            default -> List.of("id");
        };
    }

    private Long resolveOperatorId(LoginUser loginUser, Object result, Object[] args) {
        if (loginUser != null) {
            return loginUser.getUserId();
        }
        if (result instanceof Result resultVO && resultVO.getData() instanceof LoginVO loginVO) {
            return loginVO.getUserId();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        collectBizIds(args, "USER", userIds);
        return userIds.stream().findFirst().orElse(null);
    }

    private String resolveOperatorName(LoginUser loginUser, Object result, Object[] args) {
        if (loginUser != null && hasText(loginUser.getName())) {
            return loginUser.getName();
        }
        if (loginUser != null && hasText(loginUser.getUsername())) {
            return loginUser.getUsername();
        }
        if (result instanceof Result resultVO && resultVO.getData() instanceof LoginVO loginVO) {
            if (hasText(loginVO.getName())) {
                return loginVO.getName();
            }
            if (hasText(loginVO.getUsername())) {
                return loginVO.getUsername();
            }
        }
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof LoginDTO loginDTO && hasText(loginDTO.getUsername())) {
                return loginDTO.getUsername();
            }
            String operatorName = extractStringField(arg, "name", "username");
            if (hasText(operatorName)) {
                return operatorName;
            }
        }
        return null;
    }

    private Object extractFieldValue(Object source, String fieldName) {
        if (source == null || !hasText(fieldName)) {
            return null;
        }
        Class<?> current = source.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(source);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractStringField(Object source, String... fieldNames) {
        if (source == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            Object fieldValue = extractFieldValue(source, fieldName);
            if (fieldValue instanceof String stringValue && hasText(stringValue)) {
                return stringValue;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && stringValue.matches("-?\\d+")) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String trimErrorMessage(String errorMessage) {
        if (!hasText(errorMessage)) {
            return null;
        }
        return errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
