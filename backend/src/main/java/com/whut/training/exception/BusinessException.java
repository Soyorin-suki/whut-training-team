package com.whut.training.exception;

/**
 * 业务异常。
 *
 * <p>用于携带明确的业务错误码和错误信息，由全局异常处理器转换为统一响应。当前项目中大量校验失败、鉴权失败和资源不存在都会使用该异常。
 */
public class BusinessException extends RuntimeException {
    private final int code;

    /**
     * 创建业务异常。
     *
     * @param code    业务错误码。
     * @param message 错误信息。
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码。
     */
    public int getCode() {
        return code;
    }
}

