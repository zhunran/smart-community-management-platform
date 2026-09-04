package com.property.module.ai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.ai.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天历史 Mapper
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 按会话ID查询聊天历史（按时间正序，用于前端展示）
     */
    @Select("SELECT * FROM t_chat_history WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<ChatHistory> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 按用户ID查询聊天历史（最新在前）
     */
    @Select("SELECT * FROM t_chat_history WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<ChatHistory> selectByUserId(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);
}