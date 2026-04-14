package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录历史实体类
 * 对应数据库表：login_history
 */
@Data
@TableName("login_history")
public class LoginHistoryDO {
    /**
     * 记录 ID（主键）
     */
    @TableId(type = IdType.INPUT)
    private String historyId;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 登录状态：success/failed
     */
    private String status;
    
    /**
     * 登录地点
     */
    private String location;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}