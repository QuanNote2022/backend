package com.mineral.dto;

import lombok.Data;

import java.util.List;

/**
 * 检测响应 DTO
 */
@Data
public class DetectionResponse {
    /**
     * 检测记录 ID
     */
    private String detectId;
    
    /**
     * 图片 URL
     */
    private String imageUrl;
    
    /**
     * 检测结果列表
     */
    private List<DetectionResultResponse> results;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
