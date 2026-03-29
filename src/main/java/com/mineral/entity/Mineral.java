package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 矿物信息实体类
 * 对应数据库表：minerals
 */
@Data
@TableName("minerals")
public class Mineral {
    /**
     * 矿物 ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 矿物名称
     */
    private String name;
    
    /**
     * 化学式
     */
    private String formula;
    
    /**
     * 硬度
     */
    private String hardness;
    
    /**
     * 光泽
     */
    private String luster;
    
    /**
     * 颜色
     */
    private String color;
    
    /**
     * 产地
     */
    private String origin;
    
    /**
     * 用途
     */
    private String uses;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 缩略图 URL
     */
    private String thumbnail;
}
