package com.mineral.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果包装类
 * 用于封装分页查询的结果数据
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> implements Serializable {
    /**
     * 数据列表
     */
    private List<T> list;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 默认构造函数
     */
    public PageResult() {
    }

    /**
     * 带参构造函数
     * @param list 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param pageSize 每页大小
     */
    public PageResult(List<T> list, Long total, Integer page, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * 静态工厂方法
     * @param list 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param pageSize 每页大小
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> list, Long total, Integer page, Integer pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }
}
