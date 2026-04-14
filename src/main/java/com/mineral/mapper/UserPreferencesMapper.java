package com.mineral.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mineral.entity.UserPreferencesDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好设置 Mapper 接口
 */
@Mapper
public interface UserPreferencesMapper extends BaseMapper<UserPreferencesDO> {
}