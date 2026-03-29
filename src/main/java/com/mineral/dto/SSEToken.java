package com.mineral.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE（Server-Sent Events）令牌 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSEToken {
    /**
     * 连接令牌
     */
    private String token;
    
    /**
     * 是否完成
     */
    private Boolean done;
    
    /**
     * 消息 ID
     */
    private String messageId;
}
