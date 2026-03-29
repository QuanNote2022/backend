package com.mineral.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测历史查询条件 DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DetectionHistoryQuery extends com.mineral.common.PageQuery {
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 开始日期
     */
    private String startDate;
    
    /**
     * 结束日期
     */
    private String endDate;
}
