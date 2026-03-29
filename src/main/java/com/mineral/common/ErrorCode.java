package com.mineral.common;

/**
 * 错误码常量类
 * 定义系统中所有可能的错误码
 */
public class ErrorCode {
    /**
     * 成功状态码
     */
    public static final int SUCCESS = 0;
    
    /**
     * 参数错误（1000-1999）
     */
    public static final int PARAM_ERROR = 1000;
    public static final int REQUEST_FORMAT_ERROR = 1001;
    public static final int DATA_NOT_FOUND = 1002;
    public static final int DATA_EXISTS = 1003;
    
    /**
     * 认证授权错误（2000-2999）
     */
    public static final int UNAUTHORIZED = 2000;
    public static final int TOKEN_INVALID = 2001;
    public static final int TOKEN_EXPIRED = 2002;
    public static final int USERNAME_OR_PASSWORD_ERROR = 2003;
    public static final int USER_DISABLED = 2004;
    
    /**
     * 用户相关错误（3000-3999）
     */
    public static final int USERNAME_EXISTS = 3000;
    public static final int EMAIL_EXISTS = 3001;
    public static final int OLD_PASSWORD_ERROR = 3002;
    public static final int NEW_PASSWORD_FORMAT_ERROR = 3003;
    
    /**
     * 图片检测错误（4000-4999）
     */
    public static final int IMAGE_FORMAT_NOT_SUPPORTED = 4000;
    public static final int IMAGE_SIZE_EXCEEDED = 4001;
    public static final int DETECTION_FAILED = 4002;
    public static final int DETECTION_RECORD_NOT_FOUND = 4003;
    
    /**
     * 聊天会话错误（5000-5999）
     */
    public static final int SESSION_NOT_FOUND = 5000;
    public static final int SESSION_DELETED = 5001;
    public static final int MESSAGE_EMPTY = 5002;
    public static final int AI_SERVICE_UNAVAILABLE = 5003;
    
    /**
     * 文件上传错误（6000-6999）
     */
    public static final int FILE_UPLOAD_FAILED = 6000;
    public static final int FILE_TYPE_NOT_SUPPORTED = 6001;
    public static final int FILE_SIZE_EXCEEDED = 6002;
}
