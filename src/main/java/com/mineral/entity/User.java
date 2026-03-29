package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：users
 */
@Data
@TableName("users")
public class User {
    /**
     * 用户 ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码哈希
     */
    private String passwordHash;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 头像 URL
     */
    private String avatar;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 是否激活
     */
    private Boolean isActive;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}
