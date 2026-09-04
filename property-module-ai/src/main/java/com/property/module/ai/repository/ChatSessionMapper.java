package com.property.module.ai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.ai.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("SELECT * FROM t_chat_session WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<ChatSession> selectByUserId(@Param("userId") Long userId);
}