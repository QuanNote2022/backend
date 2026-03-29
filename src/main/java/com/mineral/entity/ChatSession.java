package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话实体类
 * 对应数据库表：chat_sessions
 */
@Data
@TableName("chat_sessions")
public class ChatSession {
    /**
     * 会话 ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String sessionId;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 矿物名称
     */
    private String mineralName;
    
    /**
     * 检测记录 ID
     */
    private String detectId;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}
