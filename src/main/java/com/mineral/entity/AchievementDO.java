package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就实体类
 * 对应数据库表：achievements
 */
@Data
@TableName("achievements")
public class AchievementDO {
    /**
     * 成就 ID（主键）
     */
    @TableId(type = IdType.INPUT)
    private String achievementId;
    
    /**
     * 成就名称
     */
    private String name;
    
    /**
     * 成就描述
     */
    private String description;
    
    /**
     * 成就图标（emoji或图片URL）
     */
    private String icon;
    
    /**
     * 成就等级（1-3）
     */
    private Integer level;
    
    /**
     * 成就类型
     */
    private String achievementType;
    
    /**
     * 目标值
     */
    private Integer targetValue;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}