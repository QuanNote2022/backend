package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测记录实体类
 * 对应数据库表：detections
 */
@Data
@TableName("detections")
public class DetectionDO {
    /**
     * 检测记录 ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String detectId;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 检测图片 URL
     */
    private String imageUrl;
    
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
