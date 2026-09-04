package com.property.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 对话请求
 */
@Data
public class ChatRequest {

    /** 用户ID（由服务端从安全上下文获取，不由前端传入） */
    private Long userId;

    /** 会话ID（新建会话时不传，服务端自动创建） */
    private Long sessionId;

    /** 用户消息 */
    @NotBlank(message = "消息不能为空")
    private String message;
}