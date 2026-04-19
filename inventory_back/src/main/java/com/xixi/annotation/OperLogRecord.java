package com.xixi.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLogRecord {
    String logType() default "BUSINESS";

    String moduleName();

    String operationType();

    String operationDesc() default "";

    String bizType() default "";
}
