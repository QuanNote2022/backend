package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好设置实体类
 * 对应数据库表：user_preferences
 */
@Data
@TableName("user_preferences")
public class UserPreferencesDO {
    /**
     * 用户 ID（主键）
     */
    @TableId(type = IdType.INPUT)
    private String userId;
    
    /**
     * 邮件通知开关
     */
    private Boolean emailNotification;
    
    /**
     * 系统通知开关
     */
    private Boolean systemNotification;
    
    /**
     * 界面主题：light/dark/auto
     */
    private String theme;
    
    /**
     * 语言：zh-CN/en-US
     */
    private String language;
    
    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}