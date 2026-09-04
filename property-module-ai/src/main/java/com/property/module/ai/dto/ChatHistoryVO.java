package com.property.module.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天历史响应
 */
@Data
public class ChatHistoryVO {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}