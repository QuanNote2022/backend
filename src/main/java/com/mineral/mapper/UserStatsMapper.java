package com.mineral.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mineral.entity.UserStatsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户统计 Mapper 接口
 */
@Mapper
public interface UserStatsMapper extends BaseMapper<UserStatsDO> {
}