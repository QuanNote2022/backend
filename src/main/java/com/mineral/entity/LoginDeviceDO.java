package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录设备实体类
 * 对应数据库表：login_devices
 */
@Data
@TableName("login_devices")
public class LoginDeviceDO {
    /**
     * 设备唯一标识（主键）
     */
    @TableId(type = IdType.INPUT)
    private String deviceId;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 设备类型：desktop/mobile/tablet
     */
    private String deviceType;
    
    /**
     * 操作系统
     */
    private String os;
    
    /**
     * 浏览器信息
     */
    private String browser;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 是否为当前设备
     */
    private Boolean isCurrent;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}