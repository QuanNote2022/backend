package com.mineral.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 * 统一处理系统中抛出的各种异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * @param e 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数验证异常
     * @param e 参数验证异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数验证失败");
        log.warn("Validation exception: {}", message);
        return ApiResponse.error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理参数绑定异常
     * @param e 参数绑定异常
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        log.warn("Bind exception: {}", message);
        return ApiResponse.error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理凭证错误异常
     * @param e 凭证错误异常
     * @return 统一响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ApiResponse<?> handleBadCredentialsException(BadCredentialsException e) {
        return ApiResponse.error(ErrorCode.USERNAME_OR_PASSWORD_ERROR, "用户名或密码错误");
    }

    /**
     * 处理访问 denied 异常
     * @param e 访问 denied 异常
     * @return 统一响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<?> handleAccessDeniedException(AccessDeniedException e) {
        return ApiResponse.error(ErrorCode.UNAUTHORIZED, "无权限访问");
    }

    /**
     * 处理不支持的 HTTP 请求方法
     * @param e 请求方法不支持异常
     * @return 统一响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return ApiResponse.error(ErrorCode.REQUEST_FORMAT_ERROR, "不支持的请求方法");
    }

    /**
     * 处理文件上传大小超限异常
     * @param e 文件大小超限异常
     * @return 统一响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ApiResponse.error(ErrorCode.FILE_SIZE_EXCEEDED, "文件大小超限");
    }

    /**
     * 处理其他未捕获的异常
     * @param e 异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("System exception: {}", e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误：" + e.getMessage());
    }
}
