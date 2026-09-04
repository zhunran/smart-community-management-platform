package com.property.ownerapi.controller;

import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.ai.dto.ChatHistoryVO;
import com.property.module.ai.dto.ChatRequest;
import com.property.module.ai.dto.ChatSessionVO;
import com.property.module.ai.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * AI 客服接口
 */
@Slf4j
@Tag(name = "AI 客服", description = "AI 物业客服对话接口")
@RestController
@RequestMapping(value = "/api/owner/chat", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService aiChatService;

    // ==================== 会话管理 ====================

    @Operation(summary = "创建新会话")
    @PostMapping("/sessions")
    public ApiResult<ChatSessionVO> createSession() {
        Long userId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(aiChatService.createSessionAndReturnVO(userId));
    }

    @Operation(summary = "会话列表")
    @GetMapping("/sessions")
    public ApiResult<List<ChatSessionVO>> listSessions() {
        Long userId = SecurityUtil.getLoginUser().getUserId();
        List<ChatSessionVO> list = aiChatService.listSessions(userId);
        return ApiResult.success(list);
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{id}")
    public ApiResult<Void> deleteSession(@PathVariable Long id) {
        aiChatService.deleteSession(id);
        return ApiResult.success();
    }

    // ==================== 对话 ====================

    @Operation(summary = "流式对话", description = "发送消息并接收 SSE 流式回复（打字机效果）")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> send(@Valid @RequestBody ChatRequest request) {
        Long userId = SecurityUtil.requireUser().getUserId();
        request.setUserId(userId);

        StringBuilder fullContent = new StringBuilder();
        return aiChatService.chatStream(request.getSessionId(), request)
                .doOnNext(fullContent::append)
                .doOnComplete(() -> {
                    if (!fullContent.isEmpty()) {
                        Mono.fromRunnable(() ->
                                aiChatService.saveAssistantMessage(userId, request.getSessionId(), fullContent.toString())
                        ).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    }
                })
                .doOnError(e -> {
                    log.error("SSE流式对话异常[userId={}, sessionId={}]", userId, request.getSessionId(), e);
                    if (!fullContent.isEmpty()) {
                        Mono.fromRunnable(() ->
                                aiChatService.saveAssistantMessage(userId, request.getSessionId(), fullContent + "\n[回复中断]")
                        ).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    }
                });
    }

    // ==================== 历史查询 ====================

    @Operation(summary = "聊天历史", description = "查询指定会话的聊天历史记录")
    @GetMapping("/history")
    public ApiResult<List<ChatHistoryVO>> history(@RequestParam Long sessionId) {
        List<ChatHistoryVO> list = aiChatService.getHistoryBySession(sessionId);
        return ApiResult.success(list);
    }
}