package com.mineral.dto;

import lombok.Data;

/**
 * 检测结果响应 DTO
 */
@Data
public class DetectionResultResponse {
    /**
     * 矿物标签
     */
    private String label;
    
    /**
     * 置信度
     */
    private Double confidence;
    
    /**
     * 边界框坐标 [x1, y1, x2, y2]
     */
    private Integer[] bbox;
    
    /**
     * 矿物详细信息
     */
    private MineralInfoResponse mineralInfo;
}
