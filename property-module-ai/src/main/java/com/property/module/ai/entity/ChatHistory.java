package com.property.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天历史记录
 */
@Data
@TableName("t_chat_history")
public class ChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（业主ID） */
    private Long userId;

    /** 会话ID */
    private Long sessionId;

    /** 角色：user=用户消息，assistant=AI回复 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;
}