package com.mineral.dto;

import lombok.Data;

/**
 * 聊天消息响应 DTO
 */
@Data
public class ChatMessageResponse {
    /**
     * 消息 ID
     */
    private String messageId;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 消息角色
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
