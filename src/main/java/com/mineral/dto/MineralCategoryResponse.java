package com.mineral.dto;

import lombok.Data;

/**
 * 矿物分类响应 DTO
 */
@Data
public class MineralCategoryResponse {
    /**
     * 分类 ID
     */
    private String id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 数量统计
     */
    private Integer count;
}
