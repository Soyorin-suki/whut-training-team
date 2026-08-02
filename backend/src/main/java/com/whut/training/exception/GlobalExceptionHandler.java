package com.whut.training.exception;

import com.whut.training.common.ApiResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局异常处理器。
 *
 * <p>将业务异常、参数校验异常、请求体解析异常和兜底异常统一转换为 {@link com.whut.training.common.ApiResponse}。
 * 当前项目采用“HTTP 200 + 业务码”居多的风格，因此这里负责把错误信息稳定暴露给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验、请求体读取和缺失请求头等 400 类问题。
     *
     * @param ex 被捕获的异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    public ApiResponse<Void> handleBadRequest(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException manv
                && manv.getBindingResult().getFieldError() != null) {
            return ApiResponse.fail(400, manv.getBindingResult().getFieldError().getDefaultMessage());
        }
        if (ex instanceof BindException be
                && be.getBindingResult().getFieldError() != null) {
            return ApiResponse.fail(400, be.getBindingResult().getFieldError().getDefaultMessage());
        }
        if (ex instanceof MissingRequestHeaderException mrh) {
            return ApiResponse.fail(400, "missing required header: " + mrh.getHeaderName());
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return ApiResponse.fail(400, "request body is invalid or missing");
        }
        return ApiResponse.fail(400, "invalid request");
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex 非法参数异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(400, ex.getMessage());
    }

    /**
     * 处理未显式分类的兜底异常。
     *
     * @param ex 未知异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        log.error("Unhandled request exception", ex);
        return ApiResponse.fail(500, "internal server error");
    }
}
