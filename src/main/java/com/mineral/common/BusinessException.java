package com.mineral.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private Integer code;

    /**
     * 构造业务异常（默认错误码）
     * @param message 异常消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.PARAM_ERROR;
    }

    /**
     * 构造业务异常（自定义错误码）
     * @param code 错误码
     * @param message 异常消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
