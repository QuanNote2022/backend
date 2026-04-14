package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成就实体类
 * 对应数据库表：user_achievements
 */
@Data
@TableName("user_achievements")
public class UserAchievementDO {
    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 成就 ID
     */
    private String achievementId;
    
    /**
     * 是否已解锁
     */
    private Boolean unlocked;
    
    /**
     * 解锁时间
     */
    private LocalDateTime unlockedAt;
    
    /**
     * 当前进度
     */
    private Integer progress;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}