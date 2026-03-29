package com.mineral.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mineral.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
