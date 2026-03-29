package com.mineral.dto;

import lombok.Data;

/**
 * 统计概览响应 DTO
 */
@Data
public class StatsOverviewResponse {
    /**
     * 总检测次数
     */
    private Integer totalDetections;
    
    /**
     * 总聊天次数
     */
    private Integer totalChats;
    
    /**
     * 最常检测的矿物
     */
    private String topMineral;
    
    /**
     * 周活跃天数
     */
    private Integer weeklyActiveDays;
}
