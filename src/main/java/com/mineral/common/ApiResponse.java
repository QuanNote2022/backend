package com.mineral.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应包装类
 * 用于封装所有 HTTP 请求的响应数据
 * @param <T> 响应数据类型
 */
@Data
public class ApiResponse<T> implements Serializable {
    /**
     * 响应状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（无数据）
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 成功响应（带自定义消息和数据）
     * @param message 响应消息
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /**
     * 错误响应
     * @param code 错误码
     * @param message 错误消息
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 错误响应（默认错误码）
     * @param message 错误消息
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(1000, message);
    }
}
