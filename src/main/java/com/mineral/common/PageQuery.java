package com.mineral.common;

import lombok.Data;

/**
 * 分页查询参数类
 * 用于封装分页相关的查询参数
 */
@Data
public class PageQuery {
    /**
     * 当前页码（默认第 1 页）
     */
    private Integer page = 1;
    
    /**
     * 每页大小（默认 10 条）
     */
    private Integer pageSize = 10;

    /**
     * 获取偏移量
     * @return 偏移量，用于数据库查询
     */
    public Integer getOffset() {
        return (page - 1) * pageSize;
    }

    /**
     * 获取限制数量
     * @return 每页记录数
     */
    public Integer getLimit() {
        return pageSize;
    }
}
