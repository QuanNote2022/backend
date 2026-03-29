package com.mineral.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mineral.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
