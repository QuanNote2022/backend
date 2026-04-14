package com.mineral.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mineral.entity.UserAchievementDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户成就 Mapper 接口
 */
@Mapper
public interface UserAchievementMapper extends BaseMapper<UserAchievementDO> {
}