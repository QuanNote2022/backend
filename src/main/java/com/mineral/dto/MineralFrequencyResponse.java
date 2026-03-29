package com.mineral.dto;

import lombok.Data;

/**
 * 矿物频次统计响应 DTO
 */
@Data
public class MineralFrequencyResponse {
    /**
     * 矿物名称
     */
    private String mineralName;
    
    /**
     * 出现次数
     */
    private Integer count;
}
