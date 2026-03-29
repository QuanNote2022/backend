package com.mineral.dto;

import lombok.Data;

/**
 * 创建会话请求 DTO
 */
@Data
public class CreateSessionRequest {
    /**
     * 检测记录 ID
     */
    private String detectId;
    
    /**
     * 矿物名称
     */
    private String mineralName;
}
