package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 检测结果实体类
 * 对应数据库表：detection_results
 */
@Data
@TableName("detection_results")
public class DetectionResult {
    /**
     * 主键 ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 检测记录 ID
     */
    private String detectId;
    
    /**
     * 矿物标签名称
     */
    private String label;
    
    /**
     * 置信度
     */
    private BigDecimal confidence;
    
    /**
     * 边界框 X1 坐标（左上角）
     */
    private Integer bboxX1;
    
    /**
     * 边界框 Y1 坐标（左上角）
     */
    private Integer bboxY1;
    
    /**
     * 边界框 X2 坐标（右下角）
     */
    private Integer bboxX2;
    
    /**
     * 边界框 Y2 坐标（右下角）
     */
    private Integer bboxY2;
}
