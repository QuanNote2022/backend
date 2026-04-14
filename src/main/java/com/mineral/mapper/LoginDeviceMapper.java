package com.mineral.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mineral.entity.LoginDeviceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录设备 Mapper 接口
 */
@Mapper
public interface LoginDeviceMapper extends BaseMapper<LoginDeviceDO> {
}