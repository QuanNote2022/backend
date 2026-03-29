package com.mineral.dto;

import lombok.Data;

/**
 * 聊天会话响应 DTO
 */
@Data
public class ChatSessionResponse {
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 矿物名称
     */
    private String mineralName;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
    
    /**
     * 最后活跃时间
     */
    private String lastActiveAt;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
