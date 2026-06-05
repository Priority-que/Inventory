package com.xixi.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "Result", description = "Result")
public class Result {
    @Schema(description = "统一响应编码")
    private Integer code;
    @Schema(description = "响应消息")
    private String msg;
    @Schema(description = "响应数据")
    private Object data;
    public static Result success() {
        return new Result(200, "success", null);
    }

    public static Result success(String msg) {
        return new Result(200, msg, null);
    }

    public static Result success(Object data) {
        return new Result(200, "success", data);
    }

    public static Result success(String msg, Object data) {
        return new Result(200, msg, data);
    }

    public static Result error(String msg) {
        return new Result(500, msg, null);
    }

    public static Result error(Integer code, String msg) {
        return new Result(code, msg, null);
    }
}

