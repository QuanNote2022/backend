package com.mineral.dto;

import lombok.Data;

/**
 * 登录设备响应对象
 */
@Data
public class LoginDeviceResponse {
    /**
     * 设备 ID
     */
    private String deviceId;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 设备类型
     */
    private String deviceType;
    
    /**
     * 操作系统
     */
    private String os;
    
    /**
     * 浏览器
     */
    private String browser;
    
    /**
     * 登录时间
     */
    private String loginTime;
    
    /**
     * 最后活跃时间
     */
    private String lastActiveTime;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 是否为当前设备
     */
    private Boolean isCurrent;
}