package com.property.module.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话响应
 */
@Data
public class ChatSessionVO {

    private Long id;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}