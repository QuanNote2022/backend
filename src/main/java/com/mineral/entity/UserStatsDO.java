package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户统计实体类
 * 对应数据库表：user_stats
 */
@Data
@TableName("user_stats")
public class UserStatsDO {
    /**
     * 用户 ID（主键）
     */
    @TableId(type = IdType.INPUT)
    private String userId;
    
    /**
     * 总识别次数
     */
    private Integer totalDetections;
    
    /**
     * 总问答次数
     */
    private Integer totalChats;
    
    /**
     * 活跃天数
     */
    private Integer activeDays;
    
    /**
     * 连续登录天数
     */
    private Integer consecutiveDays;
    
    /**
     * 最常识别的矿物
     */
    private String topMineral;
    
    /**
     * 识别矿物种类数
     */
    private Integer mineralTypes;
    
    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}