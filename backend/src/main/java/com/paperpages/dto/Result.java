package com.paperpages.dto;

/**
 * 统一响应包装：{ code, message, data }
 * code = 0 表示成功；非 0 表示业务/系统错误（与 HTTP 状态码同时返回）。
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
