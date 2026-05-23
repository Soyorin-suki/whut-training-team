package com.whut.training.common;

/**
 * 统一 API 响应封装。
 *
 * <p>项目所有接口均采用 {@code code/message/data} 结构返回，便于前端统一处理成功、业务失败与系统异常。
 */
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 构造成功响应。
     *
     * @param data 业务数据。
     * @param <T>  数据类型。
     * @return 成功响应，状态码固定为 200。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 构造不携带数据的成功响应。
     *
     * @return 成功响应，状态码固定为 200。
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "success", null);
    }

    /**
     * 构造失败响应。
     *
     * @param code    业务或 HTTP 风格状态码。
     * @param message 错误说明。
     * @param <T>     数据类型。
     * @return 失败响应。
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

