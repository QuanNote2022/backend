package com.mineral.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户统计响应对象
 */
@Data
public class UserStatsResponse {
    /**
     * 总识别次数
     */
    private Integer totalDetections;
    
    /**
     * 总问答次数
     */
    private Integer totalChats;
    
    /**
     * 活跃天数
     */
    private Integer activeDays;
    
    /**
     * 连续登录天数
     */
    private Integer consecutiveDays;
    
    /**
     * 最常识别的矿物
     */
    private String topMineral;
    
    /**
     * 识别矿物种类数
     */
    private Integer mineralTypes;
    
    /**
     * 周活动数据
     */
    private List<Integer> weeklyActivity;
    
    /**
     * 月活动数据
     */
    private List<Integer> monthlyActivity;
}