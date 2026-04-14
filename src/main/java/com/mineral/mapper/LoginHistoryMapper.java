package com.mineral.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mineral.entity.LoginHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录历史 Mapper 接口
 */
@Mapper
public interface LoginHistoryMapper extends BaseMapper<LoginHistoryDO> {
}