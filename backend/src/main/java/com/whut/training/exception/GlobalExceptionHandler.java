package com.whut.training.exception;

import com.whut.training.common.ApiResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(400, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        return ApiResponse.fail(500, "internal server error");
    }
}
