package com.fruitisland.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Unified API response wrapper
 *
 * <pre>
 * Success: {"code": 0, "message": "success", "data": {...}}
 * Failure: {"code": 10001, "message": "coin not enough"}
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    /** Success code */
    public static final int SUCCESS = 0;

    /** Default error code */
    public static final int ERROR = -1;

    private int code;
    private String message;
    private T data;

    // ==================== Success ====================

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS, "success", null);
    }

    // ==================== Failure ====================

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ERROR, message, null);
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, message, data);
    }

    // ==================== Convenience ====================

    public boolean isSuccess() {
        return this.code == SUCCESS;
    }
}
