package com.mineral.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 配置类
 * 配置分页插件和自动填充功能
 */
@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * 配置 MyBatis Plus 拦截器
     * 添加 MySQL 分页插件
     * 
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加 MySQL 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 插入数据时自动填充字段
     * 自动填充 createdAt、updatedAt、lastActiveAt 为当前时间
     * 
     * @param metaObject MyBatis 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 自动填充创建时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        // 自动填充更新时间
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        // 自动填充最后活跃时间
        this.strictInsertFill(metaObject, "lastActiveAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新数据时自动填充字段
     * 自动填充 updatedAt 为当前时间
     * 
     * @param metaObject MyBatis 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 自动填充更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
