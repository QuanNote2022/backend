package com.mineral.dto;

import lombok.Data;

/**
 * 成就响应对象
 */
@Data
public class AchievementResponse {
    /**
     * 成就 ID
     */
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
     * 成就图标
     */
    private String icon;
    
    /**
     * 成就等级
     */
    private Integer level;
    
    /**
     * 目标进度
     */
    private Integer targetProgress;
    
    /**
     * 是否已解锁
     */
    private Boolean unlocked;
    
    /**
     * 解锁时间
     */
    private String unlockedAt;
    
    /**
     * 当前进度
     */
    private Integer progress;
}