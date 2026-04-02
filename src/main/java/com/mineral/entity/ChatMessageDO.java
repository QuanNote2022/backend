package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 对应数据库表：chat_messages
 */
@Data
@TableName("chat_messages")
public class ChatMessageDO {
    /**
     * 消息 ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String messageId;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 消息角色（user/assistant）
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
