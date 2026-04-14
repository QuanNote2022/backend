package com.mineral.dto;

import lombok.Data;

/**
 * 登录历史响应对象
 */
@Data
public class LoginHistoryResponse {
    /**
     * 记录 ID
     */
    private String historyId;
    
    /**
     * 登录时间
     */
    private String loginTime;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 登录状态
     */
    private String status;
    
    /**
     * 登录地点
     */
    private String location;
}