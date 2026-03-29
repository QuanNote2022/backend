package com.mineral.dto;

import lombok.Data;

/**
 * 矿物信息响应 DTO
 */
@Data
public class MineralInfoResponse {
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
